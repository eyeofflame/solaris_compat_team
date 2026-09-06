package dev.efm.solaris_compat.client;

import dev.efm.solaris_compat.SolarisCompat;
import dev.efm.solaris_compat.common.entity.OliviaEntity;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;

public class OliviaRenderer extends HumanoidMobRenderer<OliviaEntity, PlayerModel<OliviaEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SolarisCompat.MODID, "textures/entity/olivia.png");

    public OliviaRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER_SLIM), true), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(OliviaEntity entity) {
        return TEXTURE;
    }
}