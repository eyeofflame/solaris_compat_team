package dev.efm.solaris_functions.client.entities.renderers;

import dev.efm.solaris_functions.SolarisFunctions;
import dev.efm.solaris_functions.client.entities.models.NuclearEntityModel;
import dev.efm.solaris_functions.common.entities.NuclearBombEntity;
import dev.efm.solaris_core.helper.GameHelper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public class NuclearEntityRenderer extends MobRenderer<NuclearBombEntity, NuclearEntityModel<NuclearBombEntity>> {
    private static final ResourceLocation TEXTURE = GameHelper.buildRes(SolarisFunctions.MODID,"textures/entity/nuclear_bomb.png");

    public NuclearEntityRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new NuclearEntityModel<>(pContext.bakeLayer(NuclearEntityModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull NuclearBombEntity pEntity) {
        return TEXTURE;
    }
}
