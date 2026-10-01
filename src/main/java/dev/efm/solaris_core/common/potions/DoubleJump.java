package dev.efm.solaris_core.common.potions;

import dev.efm.solaris_core.SolarisCore;
import dev.efm.solaris_core.common.effects.SMobEffectsRegister;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import org.jetbrains.annotations.NotNull;

public class DoubleJump extends Potion {
    public DoubleJump() {
        super("double_jump", new MobEffectInstance(SMobEffectsRegister.DOUBLE_JUMP.get(), 5 * 60 * 20, 0));
    }

    @Override
    public @NotNull String getName(@NotNull String pPrefix) {
        return pPrefix + SolarisCore.MODID + ".double_jump";
    }
}
