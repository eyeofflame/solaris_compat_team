package dev.efm.solaris_compat.coin;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

/**
 * Coins 模组（{@code coinsje}）的五个硬币档位，从铜到合金。
 *
 * <p>coinsje 在本仓库是 runtimeOnly 依赖，编译期看不到它的类，所以这里只按注册表名访问
 * （{@code coinsje:<tier>_coin} / {@code coinsje:<tier>_coin_pile}），并用它自己的物品标签
 * 判定币/堆。Coins 缺席或改 id 时所有查询返回 null / 空栈，上层按「该行不可用」降级处理。
 */
public enum CoinTier {
    COPPER, IRON, GOLD, DIAMOND, NETHERITE;

    /** 硬币堆 → 硬币的换算数量，与 Coins 自身的拆解配方（1 堆 = 9 币）一致。 */
    public static final int COINS_PER_PILE = 9;

    private static final String NAMESPACE = "coinsje";

    public static final TagKey<Item> COINS = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "coins"));
    public static final TagKey<Item> PILES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(NAMESPACE, "coin_piles"));

    /** values() 每次调用都会复制数组，缓存一份。 */
    public static final CoinTier[] VALUES = values();

    private final ResourceLocation coinId;
    private final ResourceLocation pileId;

    CoinTier() {
        String name = name().toLowerCase(Locale.ROOT);
        this.coinId = ResourceLocation.fromNamespaceAndPath(NAMESPACE, name + "_coin");
        this.pileId = ResourceLocation.fromNamespaceAndPath(NAMESPACE, name + "_coin_pile");
    }

    public ResourceLocation coinId() {
        return coinId;
    }

    public ResourceLocation pileId() {
        return pileId;
    }

    /** 对应的硬币物品；Coins 未安装时为 null。 */
    public Item coinItem() {
        return ForgeRegistries.ITEMS.getValue(coinId);
    }

    /** 对应的硬币堆物品；Coins 未安装时为 null。 */
    public Item pileItem() {
        return ForgeRegistries.ITEMS.getValue(pileId);
    }

    public ItemStack coinStack(int count) {
        Item item = coinItem();
        return item == null || count <= 0 ? ItemStack.EMPTY : new ItemStack(item, count);
    }

    /** 是否本档位的硬币。 */
    public boolean isCoin(ItemStack stack) {
        return !stack.isEmpty() && stack.is(COINS) && stack.getItem() == coinItem();
    }

    /** 是否本档位的硬币堆。 */
    public boolean isPile(ItemStack stack) {
        return !stack.isEmpty() && stack.is(PILES) && stack.getItem() == pileItem();
    }

    /** 界面上的档位名本地化键。 */
    public String langKey() {
        return "solaris_compat.coin_pouch.tier." + name().toLowerCase(Locale.ROOT);
    }

    public static CoinTier byIndex(int index) {
        return VALUES[Math.max(0, Math.min(VALUES.length - 1, index))];
    }

    /** 物品属于哪个档位（硬币或硬币堆都算）；不是 Coins 的东西返回 -1。 */
    public static int tierOf(ItemStack stack) {
        if (stack.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < VALUES.length; i++) {
            if (VALUES[i].isCoin(stack) || VALUES[i].isPile(stack)) {
                return i;
            }
        }
        return -1;
    }

    /** 一整个栈折算成硬币的数量：硬币按枚数，硬币堆按 1 堆 = 9 枚。 */
    public static int coinsFor(ItemStack stack) {
        int tier = tierOf(stack);
        if (tier < 0) {
            return 0;
        }
        return VALUES[tier].isPile(stack)
                ? stack.getCount() * COINS_PER_PILE
                : stack.getCount();
    }
}
