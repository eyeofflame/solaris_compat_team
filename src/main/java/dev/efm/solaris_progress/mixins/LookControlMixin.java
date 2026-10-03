package dev.efm.solaris_progress.mixins;

import dev.efm.rpg.entity.OliviaEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.control.LookControl;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 原版 {@link LookControl#tick()} 每 tick 会执行 {@code if (resetXRotOnTick()) mob.setXRot(0)}，
 * 把 {@code moveTo(..., pitch)} 设置的俯视角立刻清零。
 *
 * <p>这里只对 Olivia 关掉该重置：她的固定姿势（面向 -Z、俯视 30°）才能保留；
 * 其它怪物行为不变，而且 NPC 对话系统主动调用 {@code setLookAt(...)} 时依然会转动视角
 * （关闭的只是"没有目标就把 pitch 归零"这一步）。</p>
 */
@Mixin(LookControl.class)
public abstract class LookControlMixin {
    @Shadow
    @Final
    protected Mob mob;

    @Inject(method = "resetXRotOnTick", at = @At("HEAD"), cancellable = true)
    private void solaris$keepOliviaPitch(CallbackInfoReturnable<Boolean> cir) {
        if (this.mob instanceof OliviaEntity) {
            cir.setReturnValue(false);
        }
    }
}
