package dev.efm.solaris_compat.mixin.sola_trade;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void solaris$onScreen(Screen pGuiScreen, CallbackInfo ci) {
        if (pGuiScreen instanceof MerchantScreen) {
            //ci.cancel();
        }
    }
}
