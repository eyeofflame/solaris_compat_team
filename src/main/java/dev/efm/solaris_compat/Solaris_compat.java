package dev.efm.solaris_compat;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.networking.LDLNetworking;
import dev.efm.solaris_compat.common.SRegistry;
import dev.efm.solaris_compat.config.ConfigScreen;
import dev.efm.solaris_compat.config.SolarisConfig;
import dev.efm.solaris_compat.data.DataRegistry;
import dev.efm.solaris_compat.rpg_ui.RPG_GUI_API;
import dev.efm.solaris_compat.rpg_ui.SolarisUIFactory;
import dev.efm.solaris_compat.rpg_ui.network.UIPacket;
import dev.efm.solaris_compat.solarisContract.SFTBQuestsAPI;
import dev.ftb.mods.ftbquests.events.CustomRewardEvent;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
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
        fbus.addListener(this::onCommand);

        SRegistry.register(ibus);

        for (int i = 0; i < 100; i++) {
            randomListHundred.add(i);
        }

        CustomRewardEvent.EVENT.register(SFTBQuestsAPI::onRewardGot);

        UIFactory.register(SolarisUIFactory.INSTANCE);
        LDLNetworking.NETWORK.registerC2S(UIPacket.class);
    }

    public void onServerStarted(ServerStartedEvent event) {
        ServerQuestFile file = ServerQuestFile.INSTANCE;
        SFTBQuestsAPI.createFTB(file);
    }

    public void onCommand(RegisterCommandsEvent evt) {
        evt.getDispatcher().register(
                Commands.literal("std_create").executes(context -> {
                    Player player = context.getSource().getPlayer();
                    RPG_GUI_API.createTextGUI(player);
                    return 1;
                })
        );
    }
}
