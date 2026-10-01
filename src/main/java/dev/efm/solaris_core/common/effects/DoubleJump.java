package dev.efm.solaris_core.common.effects;

import net.minecraft.network.chat.Component;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import org.jetbrains.annotations.NotNull;

public class DoubleJump extends MobEffect {
    protected DoubleJump() {
        super(MobEffectCategory.BENEFICIAL, 0x4feca8);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("effect.solaris_core.double_jump");
    }
}
