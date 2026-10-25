package dev.efm.solaris_core.client.multiblock;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.efm.solaris_core.api.multiblock.SolarisMasterBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * 多方块整机渲染（GeckoLib）：注册在 master BE 类型上，部件方块本身是 INVISIBLE。
 * <p>
 * - 注册了模型 → 绕 master 中心旋转后交给 GeckoLib 渲染（支持任意尺寸和动画）
 * - 没注册 → 兜底：遍历部件偏移，逐格画 {@code part} 方块
 * <p>
 * 这里用组合而不是继承：{@link GeoBlockRenderer#render} 的签名是擦除后的
 * {@code render(BlockEntity, ...)}，直接继承会和方法类型冲突。
 */
public class SolarisMultiblockRenderer implements BlockEntityRenderer<SolarisMasterBlockEntity> {

    private final GeoBlockRenderer<SolarisMasterBlockEntity> delegate =
            new GeoBlockRenderer<>(new SolarisMultiblockGeoModel());

    public SolarisMultiblockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(SolarisMasterBlockEntity animatable, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ResourceLocation multiblockId = animatable.getMultiblockId();
        if (multiblockId == null || MultiblockModels.get(multiblockId) == null) {
            renderFallback(animatable, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        poseStack.pushPose();
        // 模型以 master 为原点，旋转绕 master 方块中心
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(-degrees(animatable.getRotation())));
        poseStack.translate(-0.5D, -0.5D, -0.5D);
        delegate.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private void renderFallback(SolarisMasterBlockEntity blockEntity, PoseStack poseStack,
                                MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockState state = blockEntity.getBlockState();
        for (BlockPos offset : blockEntity.getParts()) {
            BlockPos rotated = offset.rotate(blockEntity.getRotation());
            poseStack.pushPose();
            poseStack.translate(rotated.getX(), rotated.getY(), rotated.getZ());
            dispatcher.renderSingleBlock(state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static float degrees(Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> 90.0F;
            case CLOCKWISE_180 -> 180.0F;
            case COUNTERCLOCKWISE_90 -> 270.0F;
            default -> 0.0F;
        };
    }
}
