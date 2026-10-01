package dev.efm.solaris_core.common.potions;

import dev.efm.solaris_core.SolarisCore;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SPotionsRegister {
    public static final DeferredRegister<Potion> REGISTER = DeferredRegister.create(ForgeRegistries.POTIONS, SolarisCore.MODID);
    public static final RegistryObject<Potion> DOUBLE_JUMP = REGISTER.register("double_jump",DoubleJump::new);

    public static void register(IEventBus ibus){
        REGISTER.register(ibus);
    }
}
