package dev.efm.solaris_progress;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeEvents {
    public static final String NBT = Player.PERSISTED_NBT_TAG;

    @SubscribeEvent
    public static void onPlayerWon(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity().level().isClientSide || !event.isEndConquered()) return;

        ServerPlayer player = (ServerPlayer) event.getEntity();
        CompoundTag tag = player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG);
        if (!tag.getBoolean("sola:init_1")) {
            var stack = player.getServer().createCommandSourceStack()
                    .withSuppressedOutput()
                    .withPermission(4)
                    .withEntity(player);
            System.getLogger("91EFM").log(System.Logger.Level.INFO, String.valueOf(player.getServer().getCommands().performPrefixedCommand(stack, "clear @s")));

            SolaAPI.completeTaskForTeam(player,"7CA44A998EE0AE7C");

            tag.putBoolean("sola:init_1", true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, tag);
        }
    }

    @SubscribeEvent
    public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        if (!SolaConfig.enabled) return;
        event.getSettings().setSpawn(new BlockPos(0, (int) SolaConfig.flatY + 1, 0), 0f);
        event.setCanceled(true);
    }
}
