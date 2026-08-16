package dev.efm.solaris_compat;

import dev.efm.solaris_compat.common.SRegistry;
import dev.efm.solaris_compat.config.ConfigScreen;
import dev.efm.solaris_compat.config.SolarisConfig;
import dev.efm.solaris_compat.data.DataRegistry;
import dev.efm.solaris_compat.solarisContract.SFTBQuestsAPI;
import dev.ftb.mods.ftbquests.events.CustomRewardEvent;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Mod(Solaris_compat.MODID)
public class Solaris_compat {
    public static final String MODID = "solaris_compat";

    public static final List<Integer> randomListHundred = new ArrayList<>();
    public static final Random random = new Random();

    public Solaris_compat(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SolarisConfig.SPEC);

        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, screen) -> ConfigScreen.create(screen)
                )
        );

        var fbus = MinecraftForge.EVENT_BUS;
        var ibus = context.getModEventBus();

        fbus.addListener(this::onServerStarted);
        ibus.addListener(DataRegistry::DataRegistryEvent);
        ibus.addListener(DataRegistry::GatherDataEvent);

        SRegistry.register(ibus);

        for (int i = 0; i < 100; i++) {
            randomListHundred.add(i);
        }

        CustomRewardEvent.EVENT.register(SFTBQuestsAPI::onRewardGot);

    }

    public void onServerStarted(ServerStartedEvent event) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        SFTBQuestsAPI.createFTB(file);
    }
}
