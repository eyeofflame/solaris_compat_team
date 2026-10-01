package dev.efm.solaris_core.mixin.maxHealthFix;

import dev.efm.solaris_core.functions.maxHealthFix.IHealthFix;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.LevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @Inject(method = "respawn",at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setHealth(F)V"),locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onPlayerRespawn(ServerPlayer pPlayer, boolean pKeepEverything, CallbackInfoReturnable<ServerPlayer> cir, BlockPos blockpos, float f, boolean flag, ServerLevel serverlevel, Optional optional, ServerLevel serverlevel1, ServerPlayer serverplayer, boolean flag2, byte b0, LevelData leveldata){
        if (serverplayer instanceof IHealthFix fix){
            fix.solaris_core$setRestorePoint(pPlayer.getMaxHealth());
        }
    }
}
