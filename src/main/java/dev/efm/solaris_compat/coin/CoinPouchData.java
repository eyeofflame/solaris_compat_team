package dev.efm.solaris_compat.coin;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

/**
 * 硬币袋的持久化数据：物品 NBT 里一个 int 数组，按 {@link CoinTier} 序号存每种硬币数量。
 *
 * <p>理论上限 {@link Integer#MAX_VALUE}（对玩家即「无限」）；所有写入都做边界夹取，
 * 避免溢出成负数。键名带前缀，避免和其它 mod 往物品根标签写的键打架。
 */
public final class CoinPouchData {

    private static final String COUNTS = "SolarisCoinCounts";

    private CoinPouchData() {
    }

    public static int[] getCounts(ItemStack pouch) {
        int[] counts = new int[CoinTier.VALUES.length];
        if (pouch.isEmpty()) {
            return counts;
        }
        CompoundTag tag = pouch.getTag();
        if (tag != null && tag.contains(COUNTS, Tag.TAG_INT_ARRAY)) {
            int[] raw = tag.getIntArray(COUNTS);
            System.arraycopy(raw, 0, counts, 0, Math.min(raw.length, counts.length));
        }
        return counts;
    }

    public static int getCount(ItemStack pouch, int tier) {
        int[] counts = getCounts(pouch);
        return tier >= 0 && tier < counts.length ? Math.max(0, counts[tier]) : 0;
    }

    public static void setCount(ItemStack pouch, int tier, int value) {
        if (pouch.isEmpty() || tier < 0 || tier >= CoinTier.VALUES.length) {
            return;
        }
        int[] counts = getCounts(pouch);
        counts[tier] = Math.max(0, value);
        pouch.getOrCreateTag().putIntArray(COUNTS, counts);
    }

    /** 增减一枚额，内部按 int 边界夹取（不为负、不回绕）。 */
    public static void add(ItemStack pouch, int tier, long delta) {
        if (pouch.isEmpty() || tier < 0 || tier >= CoinTier.VALUES.length || delta == 0) {
            return;
        }
        long value = (long) getCount(pouch, tier) + delta;
        setCount(pouch, tier, (int) Math.max(0, Math.min(Integer.MAX_VALUE, value)));
    }
}
