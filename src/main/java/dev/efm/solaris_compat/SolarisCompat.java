package dev.efm.solaris_compat;

import dev.efm.solaris_compat.common.SRegistry;
import dev.efm.solaris_compat.config.ConfigScreen;
import dev.efm.solaris_compat.config.SolarisConfig;
import dev.efm.solaris_compat.data.DataRegistry;
import dev.efm.solaris_compat.events.sola_events.VillagerProfessionUpdateEvent;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod(SolarisCompat.MODID)
public class SolarisCompat {
    public static final String MODID = "solaris_compat";

    public static final List<Integer> randomListHundred = new ArrayList<>();
    public static final Random random = new Random();

    public SolarisCompat(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SolarisConfig.SPEC);

        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, screen) -> ConfigScreen.create(screen)
                )
        );

        IEventBus ibus = context.getModEventBus(), fbus = MinecraftForge.EVENT_BUS;

        ibus.addListener(DataRegistry::DataRegistryEvent);
        ibus.addListener(DataRegistry::GatherDataEvent);

        SRegistry.register(ibus);

        for (int i = 0; i < 100; i++) {
            randomListHundred.add(i);
        }

        fbus.addListener(this::onVillagerProUpdate);
    }


    //custom event
    public void onVillagerProUpdate(VillagerProfessionUpdateEvent event) {
        if (event.getLevel().isClientSide) return;

        VillagerProfession oldP = event.getOld(), newP = event.getNew();
        if (oldP.equals(VillagerProfession.NONE) && !newP.equals(VillagerProfession.NONE)) {
            String proId = newP.toString();
        }
    }
}
