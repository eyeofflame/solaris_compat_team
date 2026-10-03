package dev.efm.solaris_progress.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.efm.solaris_progress.SolaConfig;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseRouter;

/**
 * 把原版 {@link DensityFunction} 包裹成"区域内平地、边缘平滑过渡"的函数。
 * 只在主世界通过 {@link #wrapRouter(NoiseRouter)} 替换 finalDensity /
 * initialDensityWithoutJaggedness 使用。
 *
 * <p>虽然只在运行期包裹，这里仍提供一个真正的 codec（类型注册在
 * {@code solaris_progress:flat_blend}，见 {@code SolaRegistry}）：
 * 任何对 NoiseRouter 做序列化的路径都应正常编解码，而不是抛异常。</p>
 */
public final class FlatBlendDensityFunction implements DensityFunction {

    /** 1.20.1 主世界的方块 Y 范围，用于保守估计平地密度的上下界。 */
    private static final int MIN_BLOCK_Y = -64;
    private static final int MAX_BLOCK_Y = 319;

    public static final KeyDispatchDataCodec<FlatBlendDensityFunction> CODEC = KeyDispatchDataCodec.of(
            RecordCodecBuilder.mapCodec(inst -> inst.group(
                    DensityFunction.HOLDER_HELPER_CODEC.fieldOf("vanilla").forGetter(f -> f.vanilla),
                    Codec.DOUBLE.fieldOf("center_x").forGetter(f -> f.centerX),
                    Codec.DOUBLE.fieldOf("center_z").forGetter(f -> f.centerZ),
                    Codec.DOUBLE.fieldOf("flat_y").forGetter(f -> f.flatY),
                    Codec.DOUBLE.fieldOf("half_extent").forGetter(f -> f.halfExtent),
                    Codec.DOUBLE.fieldOf("blend_width").forGetter(f -> f.blendWidth)
            ).apply(inst, FlatBlendDensityFunction::new)));

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
        if (FlatBlendMath.outside(d, halfExtent, blendWidth)) {
            return vanilla.compute(ctx);
        }
        double t = FlatBlendMath.blendFactor(d, halfExtent, blendWidth);
        double flat = FlatBlendMath.flatDensity(flatY, ctx.blockY());
        if (t <= 0.0) {
            // 核心区是纯平地：不要为了丢弃结果去计算整条原版噪声（热点行）。
            return flat;
        }
        return FlatBlendMath.blend(flat, vanilla.compute(ctx), t);
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
        return Math.min(vanilla.minValue(), flatY + 1.0 - MAX_BLOCK_Y);
    }

    @Override
    public double maxValue() {
        return Math.max(vanilla.maxValue(), flatY + 1.0 - MIN_BLOCK_Y);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
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
