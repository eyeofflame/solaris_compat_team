package dev.efm.solaris_compat.mixin.sola_trade;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
    @Inject(method = "sendMerchantOffers", at = @At("HEAD"), cancellable = true)
    private void onSend(int pContainerId, MerchantOffers pOffers, int pLevel, int pXp, boolean pShowProgress, boolean pCanRestock, CallbackInfo ci) {
        ci.cancel();
    }
}
