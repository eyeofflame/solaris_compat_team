package dev.efm.solaris_compat;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.efm.rpg.SFactory;
import dev.efm.rpg.SHolder;
import dev.efm.rpg.data.Script;
import dev.efm.rpg.data.ScriptRegistry;
import dev.efm.rpg.data.ScriptReloadListener;
import dev.efm.rpg.network.RpgNetwork;
import dev.efm.solaris_compat.common.SRegistry;
import dev.efm.solaris_compat.config.ConfigScreen;
import dev.efm.solaris_compat.config.SolarisConfig;
import dev.efm.solaris_compat.data.DataRegistry;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
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

        var fbus = MinecraftForge.EVENT_BUS;
        var ibus = context.getModEventBus();

        ibus.addListener(DataRegistry::DataRegistryEvent);
        ibus.addListener(DataRegistry::GatherDataEvent);
        ibus.addListener(this::commonSetup);

        SRegistry.register(ibus);

        for (int i = 0; i < 100; i++) {
            randomListHundred.add(i);
        }

        fbus.addListener(this::onCommand);
        fbus.addListener(this::onAddReloadListeners);

        ScriptRegistry.defaultReg();
    }

    /**
     * 挂数据包剧本加载器。每次 /reload 会重新读 {@code data/<ns>/solaris_rpg/*.json}。
     */
    public void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ScriptReloadListener());
    }


    public void onCommand(RegisterCommandsEvent evt) {
        evt.getDispatcher().register(
                Commands.literal("std_create")
                        .then(Commands.argument("id", StringArgumentType.string())
                                .executes(ctx -> {
                                    String id = StringArgumentType.getString(ctx, "id");
                                    Script script = ScriptRegistry.get(id);
                                    if (script == null) {
                                        ctx.getSource().sendFailure(Component.literal("未知剧本: " + id));
                                        return 0;
                                    }
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    if (player == null) {
                                        ctx.getSource().sendFailure(Component.literal("该命令只能由玩家执行"));
                                        return 0;
                                    }
                                    SFactory.INSTANCE.openUI(new SHolder(script), player);
                                    return 1;
                                }))
        );
    }

    public void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            UIFactory.register(SFactory.INSTANCE);
            // 网络频道要在玩家进服之前注册好，两端都会跑到这里
            RpgNetwork.register();
        });
    }
}
