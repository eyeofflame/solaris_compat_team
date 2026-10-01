package dev.efm.solaris_core.mixin.noMoreString;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.TripWireHookBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TripWireHookBlock.class)
public abstract class TripWireHookBlockMixin extends Block {

    public TripWireHookBlockMixin(Properties pProperties) {
        super(pProperties);
    }

    @Redirect(method = "calculateState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean checkBeforeSet(Level instance, BlockPos pPos, BlockState pNewState, int pFlags) {
        if (instance.getBlockState(pPos).is(pNewState.getBlock())) {
            return instance.setBlock(pPos, pNewState, pFlags);
        } else {
            return false;
        }
    }
}
