package dev.efm.solaris_progress.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.efm.solaris_progress.SolaConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.EnderEyeItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EnderEyeItem.class)
public abstract class EnderEyeItemMixin {

    /**
     * 只拦"填充末地传送门方块"的 setBlock（放眼睛的那次不受影响）。
     * 用方块状态判断取代调用序号，避免对编译顺序/其它 mixin 敏感。
     */
    @WrapOperation(
            method = "useOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean solaris$blockEndPortalBlocks(Level level, BlockPos pos, BlockState state, int flags,
                                                 Operation<Boolean> original) {
        if (SolaConfig.blockEndPortalActivation && level.dimension() == Level.OVERWORLD && state.is(Blocks.END_PORTAL)) {
            return false;
        }
        return original.call(level, pos, state, flags);
    }

    /** 传送门方块没有生成时，也不应该播放传送门激活音效（1038）。 */
    @WrapOperation(
            method = "useOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;globalLevelEvent(ILnet/minecraft/core/BlockPos;I)V"))
    private void solaris$suppressPortalSound(Level level, int eventId, BlockPos pos, int data,
                                             Operation<Void> original) {
        if (SolaConfig.blockEndPortalActivation && level.dimension() == Level.OVERWORLD) {
            return;
        }
        original.call(level, eventId, pos, data);
    }
}
