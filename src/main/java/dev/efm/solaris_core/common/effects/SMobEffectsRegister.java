package dev.efm.solaris_core.common.effects;

import dev.efm.solaris_core.SolarisCore;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SMobEffectsRegister {
    public static final DeferredRegister<MobEffect> REGISTER = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, SolarisCore.MODID);
    public static final RegistryObject<MobEffect> DOUBLE_JUMP = REGISTER.register("double_jump",DoubleJump::new);

    public static void register(IEventBus ibus){
        REGISTER.register(ibus);
    }
}
