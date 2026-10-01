package dev.efm.solaris_core.common.entities;

import dev.efm.solaris_core.functions.SolarisExplosion;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import org.jetbrains.annotations.NotNull;

public class NuclearBombEntity extends Mob {
    public NuclearBombEntity(EntityType<? extends Mob> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
        this.setCustomName(Component.literal("Nuclear Bomb").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 1d);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void push(@NotNull Entity pEntity) {
    }

    @Override
    public void knockback(double pStrength, double pX, double pZ) {
    }

    @Override
    public @NotNull PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean ignoreExplosion() {
        return true;
    }

    @Override
    public void die(@NotNull DamageSource pDamageSource) {
        if (pDamageSource.getEntity() instanceof ServerPlayer player) {
            SolarisExplosion explosion = new SolarisExplosion(player.level(), this, this.getX(), this.getY(), this.getZ(), 50);
            explosion.explode();
            explosion.finalizeExplosion(true);
        }
    }
}
