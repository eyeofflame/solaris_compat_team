package dev.efm.rpg.entity;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Olivia 是剧情 NPC，不受任何伤害或指令删除影响。
 *
 * <p>这里的“无敌”包含：
 * <ul>
 *     <li>普通攻击、虚空、火焰、药水、爆炸等所有 {@link DamageSource}；</li>
 *     <li>{@code /kill}；</li>
 *     <li>依赖 {@code Entity#discard()} 的 {@code /discard} 等删除指令；</li>
 *     <li>通过 NBT / {@code /data merge} 把 {@code Health} 写成 0 或非法值。</li>
 * </ul>
 *
 * <p>区块卸载和维度迁移仍正常走 super，避免把实体永久卡在错误的世界生命周期里。
 */
public class OliviaEntity extends PathfinderMob {

    public OliviaEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 40.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D);
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isInvulnerable() {
        return true;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isDeadOrDying() {
        return false;
    }

    @Override
    public void setHealth(float health) {
        float safeHealth = (Float.isFinite(health) && health > 0.0F)
                ? Math.min(health, getMaxHealth())
                : getMaxHealth();
        super.setHealth(safeHealth);
    }

    @Override
    public void kill() {
        // /kill 和 Entity.kill() 都不能移除 Olivia。
    }

    @Override
    public void die(DamageSource source) {
        // 防御性兜底：即使其它代码直接调用 die，也不进入死亡状态。
    }

    @Override
    public void remove(Entity.RemovalReason reason) {
        if (reason == Entity.RemovalReason.KILLED || reason == Entity.RemovalReason.DISCARDED) {
            return;
        }
        super.remove(reason);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }
}
