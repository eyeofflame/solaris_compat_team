package dev.efm.solaris_resources.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.blockentity.MagicGenEntity;
import dev.efm.solaris_resources.common.registration.BlockRegistries;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

/**
 * Draws the mechanical core as Create's own shaft along the block's axis, followed by the Bedrock
 * model laid down on that same axis.
 * <p>
 * The shaft goes first so the depth buffer sorts the rod behind the cage.
 * {@code renderRotatingKineticBlock} already orients and spins the shaft for the axis, so the only
 * thing applied here is the protrusion past each end.
 * <p>
 * This wraps a {@link GeoBlockRenderer} rather than extending one: GeckoLib declares {@code render}
 * with the erased {@code BlockEntity} instead of its type variable, which makes it impossible to
 * override from a concrete subclass — both signatures collide with the interface method's erasure.
 */
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = SolarisResources.MODID, value = Dist.CLIENT)
public class MagicGenRenderer implements BlockEntityRenderer<MagicGenEntity> {

    /** How far the shaft sticks out past each end of the block, in model units (1 unit = 1/16 block). */
    private static final float PROTRUSION = 1.0F;
    private static final float SHAFT_LENGTH_SCALE = 1.0F + 2.0F * PROTRUSION / 16.0F;

    /**
     * Create's shaft is a 4-unit rod (x/z 6..10), but the gem shrinks to 3.5 units through its
     * {@code inflate} of -0.25 — so at full thickness the rod swallows the gem whole. Halving the
     * cross-section puts the rod inside the gem's silhouette, and the gem reads again.
     */
    private static final float SHAFT_THICKNESS_SCALE = 0.5F;

    static final String GEM_BONE = "gem";

    private final GeoBlockRenderer<MagicGenEntity> core = new GeoBlockRenderer<>(BlockRegistries.MAGIC_GEN_ENTITY.get()) {
        @Override
        public void preRender(PoseStack poseStack, MagicGenEntity animatable, BakedGeoModel model,
                              MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                              float partialTick, int packedLight, int packedOverlay,
                              float red, float green, float blue, float alpha) {
            super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                    partialTick, packedLight, packedOverlay, red, green, blue, alpha);
            if (isReRender) {
                return;
            }
            // The gem bone pivots on the gem's own centre, so its Y rotation spins it in place. The
            // model stays authored Y-up; the axis alignment is a pose transform, not a bone change.
            // getAngleForBe returns radians, which is the unit GeoBone rotations use.
            Direction.Axis axis = animatable.getBlockState().getValue(BlockStateProperties.AXIS);
            float angle = KineticBlockEntityRenderer.getAngleForBe(animatable, animatable.getBlockPos(), axis);
            getGeoModel().getBone(GEM_BONE).ifPresent(bone -> bone.setRotY(angle));
        }
    };

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers evt) {
        evt.registerBlockEntityRenderer(BlockRegistries.MAGIC_GEN_ENTITY.get(), context -> new MagicGenRenderer());
    }

    @Override
    public void render(MagicGenEntity animatable, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Direction.Axis axis = animatable.getBlockState().getValue(BlockStateProperties.AXIS);

        VertexConsumer shaftBuffer = bufferSource.getBuffer(RenderType.cutoutMipped());
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        shapeShaft(poseStack, axis);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        KineticBlockEntityRenderer.renderRotatingKineticBlock(animatable,
                KineticBlockEntityRenderer.shaft(axis), poseStack, shaftBuffer, packedLight);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        alignToAxis(poseStack, axis);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        this.core.render(animatable, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void shapeShaft(PoseStack poseStack, Direction.Axis axis) {
        switch (axis) {
            case X -> poseStack.scale(SHAFT_LENGTH_SCALE, SHAFT_THICKNESS_SCALE, SHAFT_THICKNESS_SCALE);
            case Z -> poseStack.scale(SHAFT_THICKNESS_SCALE, SHAFT_THICKNESS_SCALE, SHAFT_LENGTH_SCALE);
            default -> poseStack.scale(SHAFT_THICKNESS_SCALE, SHAFT_LENGTH_SCALE, SHAFT_THICKNESS_SCALE);
        }
    }

    private static void alignToAxis(PoseStack poseStack, Direction.Axis axis) {
        // Top and bottom halves of the model are mirror images, so the sign of these rotations only
        // decides which end ends up where.
        switch (axis) {
            case X -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case Z -> poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
            default -> {
            }
        }
    }
}
