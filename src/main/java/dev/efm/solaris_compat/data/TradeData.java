package dev.efm.solaris_compat.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

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
    }

    /**
     * 物品引用：{@code item} 是注册名，{@code count} 是数量，{@code nbt} 是附加数据。
     */
    public static class TradeItem {

        private final Item item;
        private final int count;
        private final CompoundTag nbt;

        public static final Codec<TradeItem> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(TradeItem::getItem),
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
    }
}
