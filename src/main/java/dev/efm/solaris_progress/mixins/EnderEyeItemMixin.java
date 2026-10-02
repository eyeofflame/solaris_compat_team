package dev.efm.solaris_progress.mixins;

import dev.efm.solaris_progress.SolaConfig;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 允许末影之眼正常填充末地传送门框架，但阻止完整图案生成 END_PORTAL 方块。
 */
@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemMixin {
    @Inject(
            method = "useOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z",
                    ordinal = 1),
            cancellable = true)
    private void solaris$blockEndPortalCreation(UseOnContext context,
                                                 CallbackInfoReturnable<InteractionResult> cir) {
        if (SolaConfig.enabled && context.getLevel().dimension() == Level.OVERWORLD) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }
}
