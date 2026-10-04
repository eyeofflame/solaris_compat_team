package dev.efm.solaris_compat.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 单个村民职业的交易配置，对应 example.json：
 * <pre>
 * {
 *   "profession": "minecraft:farmer",
 *   "trades": {
 *     "level1": {"pool": [{"need": [...], "sell": {...}, "desc": "...", "xp": 5}], "pick": 2},
 *     ...一直到 level5
 *   }
 * }
 * </pre>
 *
 * <p>可选字段的默认值：{@code trades}={}、{@code count}=1、{@code nbt}={}、
 * {@code desc}=""、{@code xp}=0。{@code trades} 可省略是为了让 {@code /sola_export}
 * 导出的模板（只有 {@code profession}）也能通过解析。
 */
public class TradeData {

    private final ResourceLocation profession;
    private final Map<String, Level> trades;

    public static final Codec<TradeData> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ResourceLocation.CODEC.fieldOf("profession").forGetter(TradeData::getProfession),
                    Codec.unboundedMap(Codec.STRING, Level.CODEC).optionalFieldOf("trades", Map.of()).forGetter(TradeData::getTrades)
            ).apply(instance, TradeData::new)
    );

    public TradeData(ResourceLocation profession, Map<String, Level> trades) {
        this.profession = profession;
        this.trades = trades;
    }

    public ResourceLocation getProfession() {
        return this.profession;
    }

    /**
     * 键为 {@code level1}..{@code level5}，对应村民的等级。
     */
    public Map<String, Level> getTrades() {
        return this.trades;
    }

    /**
     * 应用 pick：每个等级从 pool 里随机抽 {@code pick} 条，作为该村民实际提供的交易。
     * 结果里 pick 字段原样保留（服务端抽签，客户端只展示结果）。
     */
    public TradeData picked(RandomSource random) {
        Map<String, Level> result = new HashMap<>();
        this.trades.forEach((key, level) -> result.put(key, level.picked(random)));
        return new TradeData(this.profession, result);
    }

    /**
     * 某一级的交易池：{@code pick} 表示从 {@code pool} 里随机抽几条作为该村民这一级的交易。
     */
    public static class Level {

        private final List<Trade> pool;
        private final int pick;

        public static final Codec<Level> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Trade.CODEC.listOf().fieldOf("pool").forGetter(Level::getPool),
                        Codec.INT.fieldOf("pick").forGetter(Level::getPick)
                ).apply(instance, Level::new)
        );

        public Level(List<Trade> pool, int pick) {
            this.pool = pool;
            this.pick = pick;
        }

        public List<Trade> getPool() {
            return this.pool;
        }

        public int getPick() {
            return this.pick;
        }

        /**
         * 按 {@link #getPick()} 从 pool 里随机抽一份结果（村民快照用），pick 字段原样保留。
         */
        public Level picked(RandomSource random) {
            List<Trade> copy = new ArrayList<>(this.pool);
            int take = Math.min(Math.max(this.pick, 0), copy.size());
            // 部分 Fisher-Yates：把抽中的换到前 take 位
            for (int i = 0; i < take; i++) {
                int j = i + random.nextInt(copy.size() - i);
                Trade tmp = copy.get(i);
                copy.set(i, copy.get(j));
                copy.set(j, tmp);
            }
            return new Level(List.copyOf(copy.subList(0, take)), this.pick);
        }

        public CompoundTag toNbt() {
            ListTag listTag = new ListTag();
            pool.forEach(trade -> {
                listTag.add(trade.toNbt());
            });
            CompoundTag tag = new CompoundTag();
            tag.putInt("pick", pick);
            tag.put("pool", listTag);
            return tag;
        }

        public static Level fromNbt(CompoundTag tag) {
            List<Trade> pool = new ArrayList<>();
            for (Tag element : tag.getList("pool", Tag.TAG_COMPOUND)) {
                if (element instanceof CompoundTag) {
                    pool.add(Trade.fromNbt((CompoundTag) element));
                }
            }
            return new Level(pool, tag.getInt("pick"));
        }
    }

    /**
     * 一条交易模板：{@code need} 是玩家花费，{@code sell} 是产出，
     * {@code desc} 是 tooltip 翻译键，{@code xp} 是经验奖励。
     */
    public static class Trade {

        private final List<TradeItem> need;
        private final TradeItem sell;
        private final String desc;
        private final int xp;

        public static final Codec<Trade> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        TradeItem.CODEC.listOf().fieldOf("need").forGetter(Trade::getNeed),
                        TradeItem.CODEC.fieldOf("sell").forGetter(Trade::getSell),
                        Codec.STRING.optionalFieldOf("desc", "").forGetter(Trade::getDesc),
                        Codec.INT.optionalFieldOf("xp", 0).forGetter(Trade::getXp)
                ).apply(instance, Trade::new)
        );

        public Trade(List<TradeItem> need, TradeItem sell, String desc, int xp) {
            this.need = need;
            this.sell = sell;
            this.desc = desc;
            this.xp = xp;
        }

        public List<TradeItem> getNeed() {
            return this.need;
        }

        public TradeItem getSell() {
            return this.sell;
        }

        public String getDesc() {
            return this.desc;
        }

        public int getXp() {
            return this.xp;
        }

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            ListTag listTag = new ListTag();
            need.forEach(tradeItem -> {
                listTag.add(tradeItem.toNbt());
            });

            tag.put("need", listTag);
            tag.put("sell", sell.toNbt());
            tag.putString("desc", desc);
            tag.putInt("xp", xp);
            return tag;
        }

        public static Trade fromNbt(CompoundTag tag) {
            List<TradeItem> need = new ArrayList<>();
            for (Tag element : tag.getList("need", Tag.TAG_COMPOUND)) {
                if (element instanceof CompoundTag) {
                    need.add(TradeItem.fromNbt((CompoundTag) element));
                }
            }
            return new Trade(need, TradeItem.fromNbt(tag.getCompound("sell")), tag.getString("desc"), tag.getInt("xp"));
        }
    }

    /**
     * 物品引用：{@code item} 是注册名，{@code count} 是数量，{@code nbt} 是附加数据。
     */
    public static class TradeItem {

        /**
         * 物品字段的 codec：不用 {@code ForgeRegistries.ITEMS.getCodec()}——它面向注册表/压缩流场景，
         * 这里直接按注册名查表，在纯 {@code JsonOps} 下行为稳定可控。
         */
        private static final Codec<Item> ITEM_CODEC = ResourceLocation.CODEC.flatXmap(
                id -> {
                    Item item = ForgeRegistries.ITEMS.getValue(id);
                    return item == null ? DataResult.error(() -> "未知物品: " + id) : DataResult.success(item);
                },
                item -> {
                    ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                    return id == null ? DataResult.error(() -> "未注册物品: " + item) : DataResult.success(id);
                }
        );

        private final Item item;
        private final int count;
        private final CompoundTag nbt;

        public static final Codec<TradeItem> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ITEM_CODEC.fieldOf("item").forGetter(TradeItem::getItem),
                        Codec.INT.optionalFieldOf("count", 1).forGetter(TradeItem::getCount),
                        CompoundTag.CODEC.optionalFieldOf("nbt", new CompoundTag()).forGetter(TradeItem::getNbt)
                ).apply(instance, TradeItem::new)
        );

        public TradeItem(Item item, int count, CompoundTag nbt) {
            this.item = item;
            this.count = count;
            // 复制一份，避免多个实例共用同一个默认 CompoundTag
            this.nbt = nbt.copy();
        }

        public Item getItem() {
            return this.item;
        }

        public int getCount() {
            return this.count;
        }

        public CompoundTag getNbt() {
            return this.nbt;
        }

        /**
         * 生成一个全新的 ItemStack，调用方随便改都不会影响本对象。
         */
        public ItemStack toStack() {
            ItemStack stack = new ItemStack(this.item, this.count);
            if (!this.nbt.isEmpty()) {
                stack.setTag(this.nbt.copy());
            }
            return stack;
        }

        public CompoundTag toNbt() {
            return toStack().serializeNBT();
        }

        /**
         * 逆操作：{@link #toNbt()} 写的就是原版 ItemStack NBT（id/Count/tag）。
         */
        public static TradeItem fromNbt(CompoundTag tag) {
            ItemStack stack = ItemStack.of(tag);
            CompoundTag nbt = stack.getTag();
            return new TradeItem(stack.getItem(), stack.getCount(), nbt == null ? new CompoundTag() : nbt);
        }
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        tag.putString("profession", profession.toString());
        trades.forEach((string, level) -> {
            tag.put(string, level.toNbt());
        });
        return tag;
    }

    /**
     * {@link #toNbt()} 的逆操作：顶层除 {@code profession} 字符串外的 CompoundTag 值都视为等级。
     * 字段缺失或损坏时按默认值兜底（profession 解析失败为 {@code minecraft:none}），不抛异常。
     */
    public static TradeData fromNbt(CompoundTag tag) {
        ResourceLocation profession = ResourceLocation.tryParse(tag.getString("profession"));
        if (profession == null) {
            profession = ResourceLocation.withDefaultNamespace("none");
        }

        Map<String, Level> trades = new HashMap<>();
        for (String key : tag.getAllKeys()) {
            Tag element = tag.get(key);
            if (element instanceof CompoundTag) {
                trades.put(key, Level.fromNbt((CompoundTag) element));
            }
        }
        return new TradeData(profession, trades);
    }
}
