package dev.efm.solaris_core.functions;

import dev.efm.solaris_core.common.capabilities.SolarisCapabilities;
import dev.efm.solaris_core.common.commands.SolarisCommands;
import dev.efm.solaris_core.config.SolarisConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SServerEvents {
    @SubscribeEvent
    public static void ServerTicks(TickEvent.ServerTickEvent evt) {
        if (evt.phase != TickEvent.Phase.END) return;

        evt.getServer()
                .getPlayerList()
                .getPlayers()
                .forEach(serverPlayer -> {
                    if (serverPlayer.onGround() && serverPlayer.getTags().contains("efm:jumped")) {
                        serverPlayer.removeTag("efm:jumped");
                    }

                    if (serverPlayer.getMaxHealth() < serverPlayer.getHealth())serverPlayer.heal(1f);

                    serverPlayer.getCapability(SolarisCapabilities.PLAYER_DUR_DATA_CAPABILITY).ifPresent(data -> {
                        int time = data.getValue();

                        if (time > 0 && SolarisConfig.ENABLE_RESPROC.get()) {
                            Component text = Component.translatable("title.solaris_core.resProcTime").append(String.format("%.2f", time / 20f)).withStyle(ChatFormatting.YELLOW);
                            data.setValue(time - 1);
                            serverPlayer.connection.send(new ClientboundSetActionBarTextPacket(text));
                        }
                    });
                });
    }

    @SubscribeEvent
    public static void ServerStopped(ServerStoppingEvent evt) {
        SolarisCommands.S_SERVICE.shutdownNow();
        System.out.println("shutdown success");
    }
}
