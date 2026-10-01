package dev.efm.solaris_core.functions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class SolarisExplosion extends Explosion {

    // ==================== 扩散参数 ====================
    private static final int TICKS_PER_STEP = 5;     // 0.2s = 4 tick
    private static final double EXPAND_PER_STEP = 3.0;   // 每步扩展 3 格
    private boolean PARTICLES = false;

    private final double totalRadius;

    // ==================== 预收集数据 ====================
    /**
     * 按距离分组的方块壳层，每个List是一个"环"
     */
    private final List<List<BlockPos>> blockShells = new ArrayList<>();

    /**
     * 所有受影响实体
     */
    private final List<Entity> affectedEntities = new ArrayList<>();

    // ==================== 运行时状态 ====================
    private int currentStep = 0;
    private int tickCounter = 0;

    // ==================== 构造 ====================
    public SolarisExplosion(Level pLevel, @Nullable Entity pSource,
                            double pToBlowX, double pToBlowY, double pToBlowZ, float pRadius) {
        super(pLevel, pSource, pToBlowX, pToBlowY, pToBlowZ, pRadius,
                false, BlockInteraction.DESTROY);
        this.totalRadius = pRadius;
    }

    // ==================== 阶段 ①：预收集（不改动世界） ====================

    @Override
    public void explode() {
        Level level;
        try {
            level = (Level) ObfuscationReflectionHelper
                    .findField(Explosion.class, "f_46012_").get(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        Vec3 center = this.getPosition();
        double totalRadiusSq = totalRadius * totalRadius;

        // ---------- AABB 范围 ----------
        int r = (int) Math.ceil(totalRadius);
        int minX = (int) (center.x - r);
        int maxX = (int) (center.x + r);
        int minY = (int) Math.max(level.getMinBuildHeight(), center.y - r * 0.7);
        int maxY = (int) Math.min(level.getMaxBuildHeight(), center.y + r * 0.7);
        int minZ = (int) (center.z - r);
        int maxZ = (int) (center.z + r);

        // ---------- 预分配壳层 ----------
        int numShells = (int) Math.ceil(totalRadius / EXPAND_PER_STEP);
        for (int i = 0; i < numShells; i++) {
            blockShells.add(new ArrayList<>());
        }

        // ---------- 遍历方块，按距离归入对应壳层 ----------
        BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    double dx = x - center.x;
                    double dy = y - center.y;
                    double dz = z - center.z;
                    double distSq = dx * dx + dy * dy + dz * dz;

                    if (distSq > totalRadiusSq) continue;          // 超出最大半径
                    if (y < level.getMinBuildHeight() || y > level.getMaxBuildHeight()) continue;

                    mpos.set(x, y, z);
                    BlockState state = level.getBlockState(mpos);
                    if (state.isAir()) continue;

                    // ---- 抗爆计算 ----
                    float blastResistance = state.getExplosionResistance(level, mpos, this);
                    double dist = Math.sqrt(distSq);
                    double effectivePower = totalRadius - dist;

                    if (blastResistance >= 1200f) continue;        // 基岩等不可破坏
                    if (!(effectivePower > blastResistance || blastResistance == 0)) continue;

                    // ---- 放入对应壳层 ----
                    int shellIdx = (int) (dist / EXPAND_PER_STEP);
                    if (shellIdx >= numShells) shellIdx = numShells - 1;
                    blockShells.get(shellIdx).add(mpos.immutable());
                }
            }
        }

        // ---------- 收集实体 ----------
        List<Entity> list = level.getEntitiesOfClass(Entity.class,
                new AABB(minX, minY, minZ, maxX, maxY, maxZ));

        for (Entity entity : list) {
            if (entity.isSpectator()) continue;
            if (entity instanceof Player player && player.isCreative()) continue;
            affectedEntities.add(entity);
        }

        // GameEvent（原版需要）
        Field sourceField = ObfuscationReflectionHelper.findField(Explosion.class, "f_46016_");
        try {
            Entity source = (Entity) sourceField.get(this);
            level.gameEvent(source, net.minecraft.world.level.gameevent.GameEvent.EXPLODE, center);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // ★ 不在这里销毁方块！
    }

    // ==================== 阶段 ②：启动扩散 ====================

    @Override
    public void finalizeExplosion(boolean spawnParticles) {
        Level level;
        double x, y, z;
        Field levelField = ObfuscationReflectionHelper.findField(Explosion.class, "f_46012_");
        Field xField = ObfuscationReflectionHelper.findField(Explosion.class, "f_46013_");
        Field yField = ObfuscationReflectionHelper.findField(Explosion.class, "f_46014_");
        Field zField = ObfuscationReflectionHelper.findField(Explosion.class, "f_46015_");
        try {
            level = (Level) levelField.get(this);
            x = xField.getDouble(this);
            y = yField.getDouble(this);
            z = zField.getDouble(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        if (level.isClientSide) return;
        this.PARTICLES = spawnParticles;

        // 播放初始爆炸音效
        level.playSound(null, x, y, z,
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS,
                4.0F, 1.0F);

        // 注册 ServerTick 监听器
        MinecraftForge.EVENT_BUS.register(new ExplosionTicker());
    }

    // ==================== 内部 Tick 处理器 ====================

    private class ExplosionTicker {

        @SubscribeEvent
        public void onServerTick(TickEvent.ServerTickEvent event) {
            // 只在 ServerTick 的 END 阶段处理一次
            if (event.phase != TickEvent.Phase.END) return;
            if (event.side != LogicalSide.SERVER) return;

            tickCounter++;

            // 每 TICKS_PER_STEP tick 处理一个壳层
            if (tickCounter % TICKS_PER_STEP != 0) return;

            // 处理当前壳层
            processStep(currentStep);
            currentStep++;

            // 全部完成 → 注销
            if (currentStep >= blockShells.size()) {
                MinecraftForge.EVENT_BUS.unregister(this);
            }
        }
    }

    // ==================== 单步处理 ====================

    private void processStep(int step) {
        if (step >= blockShells.size()) return;

        Level level;
        try {
            level = (Level) ObfuscationReflectionHelper
                    .findField(Explosion.class, "f_46012_").get(this);
        } catch (Exception e) {
            return;
        }

        Vec3 center = this.getPosition();
        double minR = step * EXPAND_PER_STEP;                    // 本壳层的最小半径
        double maxR = Math.min((step + 1) * EXPAND_PER_STEP, totalRadius); // 最大半径

        // ------ ① 销毁当前壳层的方块 ------
        List<BlockPos> shell = blockShells.get(step);
        if (level instanceof ServerLevel serverLevel) {
            for (BlockPos pos : shell) {
                serverLevel.destroyBlock(pos, true, null);
            }
        }

        // ------ ② 伤害 / 击退当前壳层内的实体 ------
        for (Entity entity : affectedEntities) {
            Vec3 entityPos = entity.position().add(0, entity.getBbHeight() / 2, 0);
            double dist = entityPos.distanceTo(center);

            // 只处理落在此壳层范围内的实体
            if (dist < minR || dist >= maxR) continue;

            Vec3 dir = entityPos.subtract(center);
            if (dir.length() < 0.01) {
                dir = new Vec3(0, 1, 0);
            }
            dir = dir.normalize();

            // 力度（距离越近越强）
            double power = Math.max(0, 1.0 - dist / totalRadius);
            double knockback = power * 30.0;

            entity.setDeltaMovement(entity.getDeltaMovement()
                    .add(dir.x * knockback,
                            dir.y * knockback * 0.6,
                            dir.z * knockback));
            entity.hurtMarked = true;
            entity.hasImpulse = true;

            float damage = (float) (power * 1000f);
            entity.hurt(this.getDamageSource(), damage);
        }

        // ------ ③ 粒子特效（在壳层边缘生成环状粒子） ------
        if (level instanceof ServerLevel serverLevel && this.PARTICLES) {
            spawnRingParticles(serverLevel, center, maxR);
        }
    }

    // ==================== 环状粒子 ====================
    private void spawnRingParticles(ServerLevel level, Vec3 center, double radius) {
        int particleCount = (int) (radius * 8);  // 半径越大粒子越多
        for (int i = 0; i < particleCount; i++) {
            double angleH = level.random.nextDouble() * Math.PI * 2;  // 水平角
            double angleV = level.random.nextDouble() * Math.PI;       // 垂直角

            double x = center.x + radius * Math.sin(angleV) * Math.cos(angleH);
            double y = center.y + radius * Math.cos(angleV);
            double z = center.z + radius * Math.sin(angleV) * Math.sin(angleH);

            // 在大约的球面上生成爆炸粒子
            level.sendParticles(ParticleTypes.EXPLOSION,
                    x, y, z,
                    1,
                    0, 0, 0,
                    0.1);
        }

        // 中心大粒子（每步都有）
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                center.x, center.y, center.z,
                1, 0, 0, 0, 1.0);
    }
}
