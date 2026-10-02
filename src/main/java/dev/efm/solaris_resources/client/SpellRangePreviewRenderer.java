package dev.efm.solaris_resources.client;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.util.CasterUtil;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.spells.SpellEffectRange;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = SolarisResources.MODID, value = Dist.CLIENT)
public final class SpellRangePreviewRenderer {
    // Own the buffer so flushing previews never flushes another renderer's pending geometry.
    private static final MultiBufferSource.BufferSource BUFFERS =
            MultiBufferSource.immediate(new BufferBuilder(256));

    private SpellRangePreviewRenderer() { }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.screen != null
                || minecraft.options.hideGui || minecraft.player.isSpectator()) return;
        if (!(minecraft.hitResult instanceof BlockHitResult hit) || hit.getType() != HitResult.Type.BLOCK) return;

        List<SpellEffectRange.Preview> previews = previews(minecraft.player.getMainHandItem());
        if (previews.isEmpty()) previews = previews(minecraft.player.getOffhandItem());
        if (previews.isEmpty()) return;

        Vec3 camera = event.getCamera().getPosition();
        PoseStack poses = event.getPoseStack();
        poses.pushPose();
        try {
            poses.translate(-camera.x, -camera.y, -camera.z);
            for (SpellEffectRange.Preview preview : previews) {
                LevelRenderer.renderLineBox(poses, BUFFERS.getBuffer(RenderType.lines()),
                        preview.range().area(hit.getBlockPos()), preview.red(), preview.green(), preview.blue(), 0.85F);
            }
            BUFFERS.endBatch(RenderType.lines());
        } finally {
            poses.popPose();
        }
    }

    private static List<SpellEffectRange.Preview> previews(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof ICasterTool)) return List.of();
        // Preset tomes initialize empty spells in getSpellCaster. Never mutate inventory while rendering.
        var spell = CasterUtil.getCaster(stack.copy()).getSpell();
        if (spell.isEmpty()) return List.of();
        return SpellEffectRange.previews(spell.recipe.stream()
                .map(part -> part.getRegistryName().toString()).toList());
    }
}
