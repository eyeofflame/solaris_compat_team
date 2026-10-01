package dev.efm.solaris_core.client.entities.models;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.efm.solaris_core.client.entities.layers.SolarisModelLayer;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.NotNull;

public class NuclearEntityModel<T extends Mob> extends HierarchicalModel<T> {
    private final ModelPart root;
    private final ModelPart body;
    public static final ModelLayerLocation LAYER_LOCATION = SolarisModelLayer.NUCLEAR_BOMB;

    public NuclearEntityModel(ModelPart root) {
        this.root = root;
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition bb_main = partdefinition.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(0f, 0f, 0.0F, 16.0F, 26.0F, 0.0F, new CubeDeformation(0.0F)), PartPose.offset(-8.0F, -2f, 0.0F));

        return LayerDefinition.create(meshdefinition, 32, 26);
    }

    @Override
    public void setupAnim(@NotNull T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        this.body.xRot = pHeadPitch * ((float) Math.PI / 180f);
    }

    @Override
    public void renderToBuffer(@NotNull PoseStack poseStack, @NotNull VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public @NotNull ModelPart root() {
        return this.root;
    }
}