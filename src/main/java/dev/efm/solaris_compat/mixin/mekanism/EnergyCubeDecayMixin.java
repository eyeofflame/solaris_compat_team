package dev.efm.solaris_compat.mixin.mekanism;

import dev.efm.solaris_compat.api.EnergyCubeDecayHelper;
import mekanism.common.tile.TileEntityEnergyCube;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TileEntityEnergyCube.class, remap = false)
public abstract class EnergyCubeDecayMixin {

    @Unique
    private int solaris$decayTimer;

    @Inject(method = "onUpdateServer", at = @At("HEAD"))
    private void solaris$decayEnergy(CallbackInfo ci) {
        if (++solaris$decayTimer < EnergyCubeDecayHelper.DECAY_INTERVAL) {
            return;
        }
        solaris$decayTimer = 0;
        EnergyCubeDecayHelper.decay((TileEntityEnergyCube) (Object) this);
    }
}
