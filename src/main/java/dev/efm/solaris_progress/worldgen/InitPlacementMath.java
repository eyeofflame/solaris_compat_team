package dev.efm.solaris_progress.worldgen;

/**
 * init.nbt 放置的锚点计算（纯数学，便于单元测试）。
 */
public final class InitPlacementMath {
    private InitPlacementMath() {}

    /** 结构某轴原点 = 中心 - 尺寸/2（整数除法），使结构在该轴居中。 */
    public static int anchor(int center, int size) {
        return center - size / 2;
    }

    /** 底面 Y = flatY + offset。 */
    public static int anchorY(double flatY, int offset) {
        return (int) flatY + offset;
    }

    /**
     * 判断区块是否接触受保护的正方形区域。边界按方块坐标闭区间处理，
     * 允许调用方额外提供结构生成缓冲区，避免结构从区域外伸入。
     */
    public static boolean chunkIntersectsProtectedRegion(int chunkX, int chunkZ,
                                                         int centerX, int centerZ,
                                                         int halfExtent, int margin) {
        int minX = chunkX * 16;
        int maxX = minX + 15;
        int minZ = chunkZ * 16;
        int maxZ = minZ + 15;
        int protectedMinX = centerX - halfExtent - margin;
        int protectedMaxX = centerX + halfExtent + margin;
        int protectedMinZ = centerZ - halfExtent - margin;
        int protectedMaxZ = centerZ + halfExtent + margin;
        return maxX >= protectedMinX && minX <= protectedMaxX
                && maxZ >= protectedMinZ && minZ <= protectedMaxZ;
    }
}
