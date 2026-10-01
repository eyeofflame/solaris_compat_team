package dev.efm.solaris_core.mixin.maxHealthFix;

import dev.efm.solaris_core.functions.maxHealthFix.IHealthFix;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements IHealthFix {
    @Shadow
    public abstract float getMaxHealth();

    @Shadow
    public abstract float getHealth();

    @Shadow
    public abstract void setHealth(float pHealth);

    @Shadow
    public int removeArrowTime;
    @Unique
    @Nullable
    private Float actualHealth = null;

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void maxHealthFix(CompoundTag pCompound, CallbackInfo ci) {
        if (pCompound.contains("Health", 99)) {
            final Float savedHealth = pCompound.getFloat("Health");

            if (savedHealth > this.getMaxHealth() && savedHealth > 0) {
                actualHealth = savedHealth;
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void tickEFM(CallbackInfo ci) {
        if (actualHealth != null) {
            if (actualHealth > 0 && actualHealth > this.getHealth() && actualHealth < this.getMaxHealth()) {
                this.setHealth(actualHealth);
            }
            actualHealth = null;
        }
    }

    @Override
    public void solaris_core$setRestorePoint(float restorePoint) {
        this.actualHealth = restorePoint;
    }
}
