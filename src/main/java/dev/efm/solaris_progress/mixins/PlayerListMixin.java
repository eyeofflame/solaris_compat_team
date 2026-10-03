package dev.efm.solaris_progress.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.efm.solaris_progress.ForgeEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {
    @WrapOperation(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getLevel(Lnet/minecraft/resources/ResourceKey;)Lnet/minecraft/server/level/ServerLevel;"))
    private ServerLevel solaris$spawnLevel(MinecraftServer instance, ResourceKey<Level> pDimension, Operation<ServerLevel> original, @Local(ordinal = 0) CompoundTag savedData) {
        if (savedData == null && pDimension == Level.OVERWORLD) {
            ServerLevel end = instance.getLevel(Level.END);
            if (end != null) return end;
        }
        return original.call(instance, pDimension);
    }

    @Inject(method = "placeNewPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setServerLevel(Lnet/minecraft/server/level/ServerLevel;)V", shift = At.Shift.AFTER))
    private void solaris$spawnInEnd(Connection pNetManager, ServerPlayer pPlayer, CallbackInfo ci, @Local(ordinal = 0) CompoundTag savedData) {
        if (savedData != null) return; // 只有首次进入的新玩家才走末地开局
        CompoundTag per = pPlayer.getPersistentData().getCompound(ForgeEvents.NBT);
        if (per.getBoolean("sola:init_0")) return;

        ServerLevel end = pPlayer.server.getLevel(Level.END);
        if (end == null) return; // 没有末地维度时保持原版流程，避免 makeObsidianPlatform(null) 崩溃

        BlockPos pos = new BlockPos(100, 50, 0);
        pPlayer.moveTo(pos, 0f, 0f);
        pPlayer.setRespawnPosition(Level.END, pos, 0f, true, false);
        ServerLevel.makeObsidianPlatform(end);

        per.putBoolean("sola:init_0", true);
        pPlayer.getPersistentData().put(ForgeEvents.NBT, per);
    }
}
