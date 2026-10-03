package dev.efm.solaris_progress;

import dev.efm.solaris_progress.mixins.StructureTemplateAccessor;
import dev.efm.solaris_progress.worldgen.InitEntityPlacement;
import dev.efm.solaris_progress.worldgen.InitPlacementMath;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class ForgeEvents {
    public static final String NBT = Player.PERSISTED_NBT_TAG;
    private static final Logger LOGGER = LoggerFactory.getLogger("solaris_progress");

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
            LOGGER.info("clear @s -> {}", player.getServer().getCommands().performPrefixedCommand(stack, "clear @s"));

            SolaAPI.completeTaskForTeam(player, "7CA44A998EE0AE7C");

            tag.putBoolean("sola:init_1", true);
            player.getPersistentData().put(Player.PERSISTED_NBT_TAG, tag);
        }
    }

    @SubscribeEvent
    public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        if (!SolaConfig.enabled) return;
        event.getSettings().setSpawn(new BlockPos(80, (int) SolaConfig.flatY + 3, 91), 0f);
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

        // 先清掉目标区域内的旧实体：结构重试/重复放置时不会留下重复实体
        discardEntitiesInStructureArea(overworld, pos, size);

        long startedAt = Util.getMillis();
        StructurePlaceSettings settings = new StructurePlaceSettings().setIgnoreEntities(true);
        boolean placed = template.get().placeInWorld(overworld, pos, pos, settings, overworld.random, Block.UPDATE_CLIENTS);
        if (!placed) {
            log("init.nbt placement returned false at " + pos + " size " + size);
            return;
        }

        int[] entityStats = placeInitEntities(overworld, template.get(), pos, settings);
        solaris$resetVillagerMemories(overworld, pos, size);
        data.setInitPlaced(true);
        log("placed init.nbt at " + pos + " size " + size
                + " entities(placed=" + entityStats[0] + ", skipped=" + entityStats[1] + ", failed=" + entityStats[2] + ")"
                + " in " + (Util.getMillis() - startedAt) + "ms");
    }

    /** 结构放置会覆盖整个足迹，先清掉区域内非玩家实体，保证重复放置是幂等的。 */
    private static void discardEntitiesInStructureArea(ServerLevel level, BlockPos pos, Vec3i size) {
        AABB box = new AABB(
                pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + size.getX(), pos.getY() + size.getY(), pos.getZ() + size.getZ());
        int removed = 0;
        for (Entity entity : level.getEntitiesOfClass(Entity.class, box)) {
            if (entity instanceof Player) continue;
            entity.discard();
            removed++;
        }
        if (removed > 0) {
            log("cleared " + removed + " pre-existing entities in init area");
        }
    }

    /**
     * 手动放置结构实体（{@code placeInWorld} 已跳过实体）：
     * 修正挂饰的绝对坐标，跳过 NBT 为空的残缺实体，并返回统计。
     */
    private static int[] placeInitEntities(ServerLevel level, StructureTemplate template, BlockPos pos,
                                           StructurePlaceSettings settings) {
        List<StructureTemplate.StructureEntityInfo> infos = StructureTemplate.processEntityInfos(
                template, level, pos, settings,
                ((StructureTemplateAccessor) (Object) template).solaris$getEntityInfoList());
        int placed = 0;
        int skipped = 0;
        int failed = 0;
        for (StructureTemplate.StructureEntityInfo info : infos) {
            CompoundTag tag = info.nbt.copy();
            ResourceLocation entityId = InitEntityPlacement.prepareForPlacement(tag, info.pos, info.blockPos);
            if (entityId == null) {
                skipped++;
                log("skip entity with empty/invalid id at " + info.blockPos);
                continue;
            }

            Optional<Entity> created = EntityType.create(tag, level);
            if (created.isEmpty()) {
                failed++;
                log("failed to create entity " + entityId + " at " + info.blockPos);
                continue;
            }
            Entity entity = created.get();
            float yaw = entity.rotate(settings.getRotation());
            yaw += entity.mirror(settings.getMirror()) - entity.getYRot();
            entity.moveTo(info.pos.x, info.pos.y, info.pos.z, yaw, entity.getXRot());
            if (settings.shouldFinalizeEntities() && entity instanceof Mob mob) {
                mob.finalizeSpawn(level, level.getCurrentDifficultyAt(BlockPos.containing(info.pos)),
                        MobSpawnType.STRUCTURE, null, tag);
            }
            level.addFreshEntityWithPassengers(entity);
            placed++;
        }
        return new int[]{placed, skipped, failed};
    }

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
        LOGGER.info(message);
    }
}
