package dev.efm.solaris_core;

import dev.efm.solaris_core.client.ClientEventBus;
import dev.efm.solaris_core.common.SolarisRegistry;
import dev.efm.solaris_core.common.capabilities.SolarisCapabilities;
import dev.efm.solaris_core.common.commands.SolarisCommands;
import dev.efm.solaris_core.common.entities.NuclearBombEntity;
import dev.efm.solaris_core.common.network.PacketHandler;
import dev.efm.solaris_core.config.SolarisConfig;
import dev.efm.solaris_core.config.SolarisConfigScreen;
import dev.efm.solaris_core.functions.SPlayerEvents;
import dev.efm.solaris_core.functions.SServerEvents;
import dev.efm.solaris_core.functions.doubleJump.DoubleJumpHandler;
import dev.efm.solaris_core.functions.structureReplacement.DataLoader;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod("solaris_core")
public class SolarisCore {
    public static final String MODID = "solaris_core";

    public SolarisCore(FMLJavaModLoadingContext context) {
        IEventBus ibus = context.getModEventBus();
        IEventBus fbus = MinecraftForge.EVENT_BUS;

        context.registerConfig(ModConfig.Type.COMMON, SolarisConfig.SPEC);

        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, screen) -> SolarisConfigScreen.create(screen)
                )
        );

        SolarisRegistry.register(ibus);
        ibus.addListener(SolarisCapabilities::registerCapa);

        fbus.addListener(DoubleJumpHandler::handler);

        ibus.addListener(this::commonSetup);

        fbus.register(SServerEvents.class);

        fbus.addGenericListener(Entity.class, SolarisCapabilities::onAttach);
        fbus.register(SPlayerEvents.class);

        fbus.addListener(SolarisCommands::onRegisterCommands);
        //fbus.addListener(UIPacket::WindowChangeEvent);

        fbus.addListener(DataLoader::onReload);

        ibus.addListener(this::registerAttributes);
        ibus.register(ClientEventBus.class);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }

    private void registerAttributes(EntityAttributeCreationEvent evt) {
        evt.put(SolarisRegistry.NUCLEAR_BOMB.get(), NuclearBombEntity.createAttributes().build());
    }
}
