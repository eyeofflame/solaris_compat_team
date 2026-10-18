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
        if (!isReRender) {
            poseStack.translate(0.0F, -0.5F, 0.0F);
        }
        getGeoModel().getBone(MagicGenRenderer.GEM_BONE).ifPresent(bone -> bone.setRotY(0.0F));
    }
}
