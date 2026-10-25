package dev.efm.solaris_functions;

import dev.efm.solaris_functions.client.ClientEventBus;
import dev.efm.solaris_functions.common.SolarisFunctionsRegistry;
import dev.efm.solaris_functions.common.capabilities.SolarisCapabilities;
import dev.efm.solaris_functions.common.commands.SolarisCommands;
import dev.efm.solaris_functions.common.network.PacketHandler;
import dev.efm.solaris_functions.config.SolarisConfig;
import dev.efm.solaris_functions.config.SolarisConfigScreen;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SolarisFunctions.MODID)
public class SolarisFunctions {
    public static final String MODID = "solaris_functions";

    public SolarisFunctions(FMLJavaModLoadingContext context) {
        IEventBus ibus = context.getModEventBus();
        IEventBus fbus = MinecraftForge.EVENT_BUS;

        context.registerConfig(ModConfig.Type.COMMON, SolarisConfig.SPEC);

        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, screen) -> SolarisConfigScreen.create(screen)
                )
        );

        SolarisFunctionsRegistry.register(ibus);
        ibus.addListener(SolarisCapabilities::registerCapa);

        ibus.addListener(this::commonSetup);
        ibus.addListener(this::registerAttributes);

        fbus.register(SServerEvents.class);

        fbus.addGenericListener(Entity.class, SolarisCapabilities::onAttach);
        fbus.register(SPlayerEvents.class);

        fbus.addListener(SolarisCommands::onRegisterCommands);

        ibus.register(ClientEventBus.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }

    private void registerAttributes(EntityAttributeCreationEvent evt) {
        evt.put(SolarisFunctionsRegistry.NUCLEAR_BOMB.get(), dev.efm.solaris_functions.common.entities.NuclearBombEntity.createAttributes().build());
    }
}
