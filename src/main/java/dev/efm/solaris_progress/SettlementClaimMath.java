package dev.efm.solaris_progress;

/**
 * 聚落领地区域的区块范围计算（纯数学，便于单元测试）。
 */
public final class SettlementClaimMath {
    public static final int CHUNK_SIZE = 16;

    private SettlementClaimMath() {}

    /** 区域最小方块坐标（闭区间）= center - halfExtent。 */
    public static int minBlock(int center, int halfExtent) {
        return center - halfExtent;
    }

    /** 区域最大方块坐标（闭区间）= center + halfExtent - 1。 */
    public static int maxBlock(int center, int halfExtent) {
        return center + halfExtent - 1;
    }

    /** 覆盖区域的最小区块。必须用 floorDiv：负数时向零截断的 / 会算错。 */
    public static int minChunk(int center, int halfExtent) {
        return Math.floorDiv(minBlock(center, halfExtent), CHUNK_SIZE);
    }

    /** 覆盖区域的最大区块。 */
    public static int maxChunk(int center, int halfExtent) {
        return Math.floorDiv(maxBlock(center, halfExtent), CHUNK_SIZE);
    }

    /** 某轴的区块数。 */
    public static int chunkCount(int center, int halfExtent) {
        return maxChunk(center, halfExtent) - minChunk(center, halfExtent) + 1;
    }
}
