package dev.efm.solaris_core.mixin.noXpCooldown;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExperienceOrb.class)
public abstract class ExperienceMixin extends Entity {
    @Shadow
    private int count;

    @Shadow
    protected abstract int repairPlayerItems(Player pPlayer, int pRepairAmount);

    @Shadow
    public abstract int getValue();

    public ExperienceMixin(EntityType<?> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "playerTouch", at = @At("HEAD"))
    private void addXp(Player pEntity, CallbackInfo ci) {
        if (!pEntity.level().isClientSide) {
            pEntity.takeXpDelay = 0;
            while (this.count>1){
                int remainder = this.repairPlayerItems(pEntity,this.getValue());
                if (remainder>0){
                    pEntity.giveExperiencePoints(remainder);
                }
                this.count--;
            }
        }
    }
}
