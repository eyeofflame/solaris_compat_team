package dev.efm.solaris_compat.mixin.corpseFix;

import com.llamalad7.mixinextras.sugar.Local;
import de.maxhenkel.corpse.events.DeathEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Objects;

@Mixin(value = DeathEvents.class, remap = false)
public abstract class DeathEventsMixin {
    @Redirect(method = "playerDeath", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerLevel;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true), remap = false)
    private boolean fixVoidCorpse(ServerLevel instance, Entity pEntity, @Local(name = "player") ServerPlayer player) {
        if (player.position().y < instance.getMinBuildHeight()) {
            ServerLevel overworld = player.server.overworld();
            var position = player.getRespawnPosition() == null ? overworld.getSharedSpawnPos() : player.getRespawnPosition();
            var dimension = player.getRespawnDimension();

            pEntity.changeDimension(Objects.requireNonNull(player.server.getLevel(dimension)));
            pEntity.moveTo(position, pEntity.getYRot(), pEntity.getXRot());
            player.sendSystemMessage(Component.translatable("tip.solaris.corpse_void_fix").withStyle(ChatFormatting.AQUA).append(Component.translatable("dimension." + dimension.location().getNamespace() + "." + dimension.location().getPath()).append(String.format(" %d,%d,%d", position.getX(), position.getY(), position.getZ())).withStyle(ChatFormatting.YELLOW)));
            return Objects.requireNonNull(player.server.getLevel(dimension)).addFreshEntity(pEntity);
        }
        return instance.addFreshEntity(pEntity);
    }
}
