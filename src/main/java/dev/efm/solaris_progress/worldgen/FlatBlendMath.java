package dev.efm.solaris_progress.worldgen;

/**
 * 纯数学工具：坐标/混合计算，不依赖任何 Minecraft 类，便于单元测试。
 */
public final class FlatBlendMath {
    private FlatBlendMath() {}

    /** 平滑阶跃：u<=0 → 0，u>=1 → 1，中间单调。 */
    public static double smoothstep(double u) {
        return u * u * (3.0 - 2.0 * u);
    }

    /** 切比雪夫距离：max(|dx|,|dz|)，使区域为正方形。 */
    public static double chebyshev(double dx, double dz) {
        return Math.max(Math.abs(dx), Math.abs(dz));
    }

    /** 混合系数：核心内 0，混合带外 1，带内 smoothstep。 */
    public static double blendFactor(double d, double halfExtent, double blendWidth) {
        if (blendWidth <= 0.0) return d < halfExtent ? 0.0 : 1.0;
        double u = (d - halfExtent) / blendWidth;
        if (u <= 0.0) return 0.0;
        if (u >= 1.0) return 1.0;
        return smoothstep(u);
    }

    /** 完全在混合带之外（可与原版逐位一致）。 */
    public static boolean outside(double d, double halfExtent, double blendWidth) {
        return d >= halfExtent + blendWidth;
    }

    /** 目标平地密度：y<=flatY 为正（实心），y>flatY 为负（空气），在 flatY 处交叉。 */
    public static double flatDensity(double flatY, double blockY) {
        return flatY + 1.0 - blockY;
    }

    /** 线性插值：t=0 取 flat，t=1 取 vanilla。 */
    public static double blend(double flat, double vanilla, double t) {
        return flat + (vanilla - flat) * t;
    }

    /**
     * 核心决策：区域外返回原版密度；区域内与目标平地密度按距离平滑混合。
     * 抽成纯函数以便单元测试。
     */
    public static double resolve(double d, double halfExtent, double blendWidth,
                                 double flatY, double blockY, double vanillaValue) {
        if (outside(d, halfExtent, blendWidth)) {
            return vanillaValue;
        }
        double t = blendFactor(d, halfExtent, blendWidth);
        return blend(flatDensity(flatY, blockY), vanillaValue, t);
    }

    /** 边界抖动，返回 [-2,2] 的确定性伪随机偏移，避免正方形硬边。 */
    public static double edgeDither(int x, int z) {
        int h = x * 374761393 + z * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);
        return ((h & 0xFF) / 255.0 - 0.5) * 4.0;
    }
}
