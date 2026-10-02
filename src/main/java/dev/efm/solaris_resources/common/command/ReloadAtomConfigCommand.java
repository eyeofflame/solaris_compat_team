package dev.efm.solaris_resources.common.command;

import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.config.AtomConfigs;
import dev.efm.solaris_resources.common.worldgen.OreGenerationRules;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SolarisResources.MODID)
public class ReloadAtomConfigCommand {
    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("solaris_resources_reload")
                .requires(source -> source.hasPermission(2))
                .executes(context -> {
                    var result = AtomConfigs.STORE.reload();
                    boolean worldgenLoaded = OreGenerationRules.reload();
                    context.getSource().sendSuccess(() -> Component.translatable(
                            "commands.solaris_resources.reload", result.loaded(), result.failed()), true);
                    context.getSource().sendSuccess(() -> Component.translatable(worldgenLoaded
                            ? "commands.solaris_resources.worldgen_reload.success"
                            : "commands.solaris_resources.worldgen_reload.failed"), true);
                    return result.failed() == 0 && worldgenLoaded ? 1 : 0;
                }));
    }
}
