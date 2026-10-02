package dev.efm.solaris_resources.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.efm.solaris_resources.common.entity.AtomItemEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AtomItemEntityRenderer extends EntityRenderer<AtomItemEntity> {

    private final ItemRenderer itemRenderer;

    public AtomItemEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
        this.shadowRadius = 0.15F;
        this.shadowStrength = 0.75F;
    }

    @Override
    public void render(AtomItemEntity entity, float entityYaw, float partialTicks, @NotNull PoseStack pose,
                       @NotNull MultiBufferSource buffer, int packedLight) {
        ItemStack stack = entity.getItem();
        if (stack.isEmpty()) {
            return;
        }

        pose.pushPose();
        BakedModel model = this.itemRenderer.getModel(stack, entity.level(), null, entity.getId());
        float bob = Mth.sin(((float) entity.getAge() + partialTicks) / 10.0F + entity.bobOffs) * 0.1F + 0.1F;
        float scaleY = model.getTransforms().getTransform(ItemDisplayContext.GROUND).scale.y();
        pose.translate(0.0F, bob + 0.25F * scaleY, 0.0F);
        pose.mulPose(Axis.YP.rotation(entity.getSpin(partialTicks)));
        this.itemRenderer.render(stack, ItemDisplayContext.GROUND, false, pose, buffer,
                packedLight, OverlayTexture.NO_OVERLAY, model);
        pose.popPose();

        super.render(entity, entityYaw, partialTicks, pose, buffer, packedLight);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull AtomItemEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
