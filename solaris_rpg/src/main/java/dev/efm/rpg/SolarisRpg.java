package dev.efm.rpg;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.efm.rpg.data.Script;
import dev.efm.rpg.data.ScriptRegistry;
import dev.efm.rpg.data.ScriptReloadListener;
import dev.efm.rpg.network.RpgNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SolarisRpg.MODID)
public class SolarisRpg {
    public static final String MODID = "solaris_rpg";

    public SolarisRpg(FMLJavaModLoadingContext context) {
        var fbus = MinecraftForge.EVENT_BUS;
        var ibus = context.getModEventBus();

        ibus.addListener(this::commonSetup);

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
