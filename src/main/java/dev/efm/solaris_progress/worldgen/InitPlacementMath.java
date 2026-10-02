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
}
