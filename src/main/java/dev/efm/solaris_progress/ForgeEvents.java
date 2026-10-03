package dev.efm.solaris_progress;

import dev.efm.rpg.entity.OliviaEntities;
import dev.efm.rpg.entity.OliviaEntity;
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

    /**
     * 出生/重生的默认朝向：面向 +Z。
     *
     * <p>Minecraft 的 yaw 约定是 0 = 南（+Z）、90 = 西（-X）、180 = 北（-Z）、270 = 东（+X），
     * 见 {@code Direction.fromYRot} 的注释与 {@code Direction.SOUTH} 的法向量 {@code (0, 0, 1)}。</p>
     */
    public static final float SPAWN_YAW_POSITIVE_Z = 0.0F;

    /**
     * Olivia 的固定朝向：面向 -Z（北）、俯视 30°。
     *
     * <p>MC 中 yaw 0 = +Z、180 = -Z；pitch 正值为向下，
     * {@code Vec3.directionFromRotation(30, 180)} = {@code (0, -0.5, -0.866)}。</p>
     */
    public static final float OLIVIA_YAW_FACING_NEGATIVE_Z = 180.0F;
    public static final float OLIVIA_PITCH_DOWN = 30.0F;

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
        event.getSettings().setSpawn(new BlockPos(80, (int) SolaConfig.flatY + 3, 91), SPAWN_YAW_POSITIVE_Z);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!SolaConfig.enabled || !SolaConfig.placeInit) return;
        MinecraftServer server = event.getServer();
        ServerLevel overworld = server.overworld();

        // 老存档里已经存在的 Olivia 也要纠正姿势（她的旧 pitch 已经被原版归零了）
        solaris$pinOliviaPose(overworld);

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

        OliviaEntity olivia = OliviaEntities.OLIVIA.create(overworld);
        if (olivia != null) {
            olivia.moveTo(new BlockPos(80, (int) SolaConfig.flatY + 4, 96),
                    OLIVIA_YAW_FACING_NEGATIVE_Z, OLIVIA_PITCH_DOWN);
            solaris$applyOliviaPose(olivia);
            overworld.addFreshEntity(olivia);
            log("spawned olivia at " + olivia.blockPosition());
        }

    }

    /**
     * Olivia 的固定姿势：面向 -Z、俯视 30°，并要求持久化（免得走远后被原版 despawn）。
     * pitch 能保持住依赖 {@code LookControlMixin} 对 Olivia 关闭每 tick 的 xRot 归零。
     */
    private static void solaris$applyOliviaPose(OliviaEntity olivia) {
        olivia.setPersistenceRequired();
        olivia.setYRot(OLIVIA_YAW_FACING_NEGATIVE_Z);
        olivia.setXRot(OLIVIA_PITCH_DOWN);
        olivia.setYHeadRot(OLIVIA_YAW_FACING_NEGATIVE_Z);
        olivia.setYBodyRot(OLIVIA_YAW_FACING_NEGATIVE_Z);
    }

    /** 基地附近已存在的 Olivia 统一纠正姿势（服务端启动时跑一次，幂等）。 */
    private static void solaris$pinOliviaPose(ServerLevel overworld) {
        AABB box = new AABB(48.0, 0.0, 64.0, 112.0, 256.0, 128.0);
        int pinned = 0;
        for (OliviaEntity olivia : overworld.getEntitiesOfClass(OliviaEntity.class, box)) {
            solaris$applyOliviaPose(olivia);
            pinned++;
        }
        if (pinned > 0) {
            log("pinned olivia pose for " + pinned + " entity/entities");
        }
    }

    /**
     * 结构放置会覆盖整个足迹，先清掉区域内非玩家实体，保证重复放置是幂等的。
     */
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
