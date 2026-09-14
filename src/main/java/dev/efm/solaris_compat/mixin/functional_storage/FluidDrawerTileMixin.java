package dev.efm.solaris_compat.mixin.functional_storage;

import com.buuz135.functionalstorage.block.tile.FluidDrawerTile;
import dev.efm.solaris_compat.api.WaterGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FluidDrawerTile.class, remap = false)
public class FluidDrawerTileMixin {
    @Inject(method = "serverTick(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/buuz135/functionalstorage/block/tile/FluidDrawerTile;)V", at = @At("TAIL"), remap = false)
    private void genWater(Level level, BlockPos pos, BlockState stateOwn, FluidDrawerTile blockEntity, CallbackInfo ci) {
        WaterGenerator.tick((FluidDrawerTile) (Object) this);
    }
}
