package dev.efm.solaris_progress.worldgen;

import dev.efm.solaris_progress.SolaConfig;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;

/**
 * 把原版 {@link DensityFunction} 包裹成"区域内平地、边缘平滑过渡"的函数。
 * 只在主世界通过 {@link #wrapRouter(NoiseRouter)} 替换 finalDensity /
 * initialDensityWithoutJaggedness 使用；运行时对象，不参与序列化。
 */
public final class FlatBlendDensityFunction implements DensityFunction {
    private final DensityFunction vanilla;
    private final double centerX;
    private final double centerZ;
    private final double flatY;
    private final double halfExtent;
    private final double blendWidth;

    public FlatBlendDensityFunction(DensityFunction vanilla, double centerX, double centerZ,
                                    double flatY, double halfExtent, double blendWidth) {
        this.vanilla = vanilla;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.flatY = flatY;
        this.halfExtent = halfExtent;
        this.blendWidth = blendWidth;
    }

    @Override
    public double compute(FunctionContext ctx) {
        double d = FlatBlendMath.chebyshev(ctx.blockX() - centerX, ctx.blockZ() - centerZ);
        return FlatBlendMath.resolve(d, halfExtent, blendWidth, flatY, ctx.blockY(), vanilla.compute(ctx));
    }

    @Override
    public void fillArray(double[] array, ContextProvider provider) {
        for (int i = 0; i < array.length; i++) {
            array[i] = compute(provider.forIndex(i));
        }
    }

    @Override
    public DensityFunction mapAll(Visitor visitor) {
        // 必须递归：让 NoiseChunk 仍能给内部原版密度挂上插值/缓存。
        DensityFunction mapped = vanilla.mapAll(visitor);
        return visitor.apply(new FlatBlendDensityFunction(
                mapped, centerX, centerZ, flatY, halfExtent, blendWidth));
    }

    @Override
    public double minValue() {
        return Math.min(vanilla.minValue(), flatY + 1.0 - 320.0);
    }

    @Override
    public double maxValue() {
        return Math.max(vanilla.maxValue(), flatY + 1.0 + 64.0);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        throw new UnsupportedOperationException("runtime-only density function");
    }

    /** 用当前配置包裹主世界 router 的 initialDensityWithoutJaggedness 与 finalDensity。 */
    public static NoiseRouter wrapRouter(NoiseRouter router) {
        double cx = SolaConfig.centerX;
        double cz = SolaConfig.centerZ;
        double fy = SolaConfig.flatY;
        double r = SolaConfig.halfExtent;
        double w = SolaConfig.blendWidth;

        DensityFunction initial = new FlatBlendDensityFunction(
                router.initialDensityWithoutJaggedness(), cx, cz, fy, r, w);
        DensityFunction fin = new FlatBlendDensityFunction(
                router.finalDensity(), cx, cz, fy, r, w);

        return new NoiseRouter(
                router.barrierNoise(),
                router.fluidLevelFloodednessNoise(),
                router.fluidLevelSpreadNoise(),
                router.lavaNoise(),
                router.temperature(),
                router.vegetation(),
                router.continents(),
                router.erosion(),
                router.depth(),
                router.ridges(),
                initial,
                fin,
                router.veinToggle(),
                router.veinRidged(),
                router.veinGap());
    }
}
