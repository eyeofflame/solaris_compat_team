package dev.efm.solaris_core.client.multiblock;

import dev.efm.solaris_core.api.multiblock.SolarisMasterBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/**
 * 按图纸 id 动态选择 GeckoLib 模型的 {@link GeoModel}。
 */
public class SolarisMultiblockGeoModel extends GeoModel<SolarisMasterBlockEntity> {

    private static final ResourceLocation FALLBACK_MODEL = ResourceLocation.fromNamespaceAndPath("solaris_core", "geo/multiblock/example.geo.json");
    private static final ResourceLocation FALLBACK_TEXTURE = ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/iron_block.png");
    private static final ResourceLocation FALLBACK_ANIMATION = ResourceLocation.fromNamespaceAndPath("solaris_core", "animations/multiblock/empty.animation.json");

    @Override
    public ResourceLocation getModelResource(SolarisMasterBlockEntity animatable) {
        MultiblockModels.MultiblockModel model = MultiblockModels.get(animatable.getMultiblockId());
        return model != null ? model.model() : FALLBACK_MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(SolarisMasterBlockEntity animatable) {
        MultiblockModels.MultiblockModel model = MultiblockModels.get(animatable.getMultiblockId());
        return model != null ? model.texture() : FALLBACK_TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(SolarisMasterBlockEntity animatable) {
        MultiblockModels.MultiblockModel model = MultiblockModels.get(animatable.getMultiblockId());
        return model != null && model.animation() != null ? model.animation() : FALLBACK_ANIMATION;
    }
}
