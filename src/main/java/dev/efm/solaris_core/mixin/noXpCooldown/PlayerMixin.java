package dev.efm.solaris_core.mixin.noXpCooldown;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Shadow
    protected abstract void touch(Entity pEntity);

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private boolean processXpOrb(List instance, Object e) {
        Entity entity = (Entity) e;
        if (entity.getType().equals(EntityType.EXPERIENCE_ORB)) {
            this.touch(entity);
            return true;
        }
        return instance.add(entity);
    }
}
