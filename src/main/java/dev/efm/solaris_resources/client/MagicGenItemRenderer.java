package dev.efm.solaris_resources.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.item.MagicGenItem;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;

/**
 * Renders the item form from the same Bedrock geometry the placed block uses.
 * <p>
 * The .geo follows the block convention: centred on x/z, sitting on y=0. GeckoLib's item renderer
 * leaves the model origin at the item origin (its own +0.5 translate cancels the -0.5 that
 * {@code ItemRenderer} applies), so a block-shaped model ends up half a block too high in the GUI
 * and in hand. Drop it by half a block to centre it; that is the only axis the block convention
 * disagrees with GeckoLib's item convention on.
 */
public class MagicGenItemRenderer extends GeoItemRenderer<MagicGenItem> {

    public MagicGenItemRenderer() {
        super(new DefaultedBlockGeoModel<>(
                ResourceLocation.fromNamespaceAndPath(SolarisResources.MODID, "unstable_machanical_core")));
    }

    @Override
    public void preRender(PoseStack poseStack, MagicGenItem animatable, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender,
                partialTick, packedLight, packedOverlay, red, green, blue, alpha);
        // Matches the guard on GeoItemRenderer's own translate: only the first pass moves the pose.
        if (!isReRender) {
            poseStack.translate(0.0F, -0.5F, 0.0F);
        }
        // Baked models are shared across renderers, so the block renderer's per-frame gem spin would
        // otherwise leak into the item. The item has no kinetic speed, so hold the gem still.
        getGeoModel().getBone(MagicGenRenderer.GEM_BONE).ifPresent(bone -> bone.setRotY(0.0F));
    }
}
