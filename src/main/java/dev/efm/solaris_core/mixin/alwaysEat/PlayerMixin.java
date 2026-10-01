package dev.efm.solaris_core.mixin.alwaysEat;

import dev.efm.solaris_core.config.SolarisConfig;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "canEat", at = @At("HEAD"), cancellable = true)
    private void alwaysCanEat(boolean pCanAlwaysEat, CallbackInfoReturnable<Boolean> cir) {
        if (SolarisConfig.ENABLE_EAT.get()) cir.setReturnValue(true);
    }
}
