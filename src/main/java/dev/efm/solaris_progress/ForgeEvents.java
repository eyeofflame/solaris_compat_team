package dev.efm.solaris_progress;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import dev.efm.solaris_progress.worldgen.InitPlacementMath;

import java.util.Optional;

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

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!SolaConfig.enabled || !SolaConfig.placeInit) return;
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();
        SolaWorldData data = SolaWorldData.get(overworld);
        if (data.isInitPlaced()) return;

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(SolaProgress.MODID, "init");
        Optional<StructureTemplate> template = server.getStructureManager().get(id);
        if (template.isEmpty()) {
            log("init.nbt not found: " + id + " (skip placement)");
            return;
        }

        Vec3i size = template.get().getSize();
        BlockPos pos = new BlockPos(
                InitPlacementMath.anchor((int) SolaConfig.centerX, size.getX()),
                InitPlacementMath.anchorY(SolaConfig.flatY, SolaConfig.initYOffset),
                InitPlacementMath.anchor((int) SolaConfig.centerZ, size.getZ()));

        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(false);
        boolean placed = template.get().placeInWorld(overworld, pos, pos, settings, overworld.random, Block.UPDATE_CLIENTS);
        if (!placed) {
            log("init.nbt placement returned false at " + pos + " size " + size);
            return;
        }

        solaris$resetVillagerMemories(overworld, pos, size);
        data.setInitPlaced(true);
        log("placed init.nbt at " + pos + " size " + size);
    }

    /** 村民 Brain 的空间记忆是绝对坐标，放置后清擦，让其重新认领新位置。 */
    private static void solaris$resetVillagerMemories(ServerLevel level, BlockPos pos, Vec3i size) {
        AABB box = new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + size.getX(), pos.getY() + size.getY(), pos.getZ() + size.getZ());
        for (Villager villager : level.getEntitiesOfClass(Villager.class, box)) {
            var brain = villager.getBrain();
            brain.eraseMemory(MemoryModuleType.HOME);
            brain.eraseMemory(MemoryModuleType.JOB_SITE);
            brain.eraseMemory(MemoryModuleType.POTENTIAL_JOB_SITE);
            brain.eraseMemory(MemoryModuleType.SECONDARY_JOB_SITE);
            brain.eraseMemory(MemoryModuleType.MEETING_POINT);
            brain.eraseMemory(MemoryModuleType.LAST_SLEPT);
            brain.eraseMemory(MemoryModuleType.LAST_WOKEN);
        }
    }

    private static void log(String message) {
        System.getLogger("91EFM").log(System.Logger.Level.INFO, "[solaris_progress] " + message);
    }
}
