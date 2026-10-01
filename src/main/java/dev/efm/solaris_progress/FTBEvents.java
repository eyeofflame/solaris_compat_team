package dev.efm.solaris_progress;

import dev.architectury.event.EventResult;
import dev.ftb.mods.ftbquests.events.CustomRewardEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;

public class FTBEvents {
    public static EventResult SolaCustomReward(CustomRewardEvent event) {
        ServerPlayer player = event.getPlayer();
        if (event.getReward().getTags().contains("sola_init_kill")) {
            player.serverLevel().getAllEntities().forEach(entity -> {
                if (entity.getType().equals(EntityType.ENDER_DRAGON)) {
                    entity.hurt(player.damageSources().playerAttack(player), 100000);
                }
            });
        } else if (event.getReward().getTags().contains("sola_init_0")) {
            player.setRespawnPosition(player.server.overworld().dimension(), player.server.overworld().getSharedSpawnPos(), player.getRespawnAngle(), true, false);
        }
        return EventResult.pass();
    }

    public static void init() {
        CustomRewardEvent.EVENT.register(FTBEvents::SolaCustomReward);
    }
}
