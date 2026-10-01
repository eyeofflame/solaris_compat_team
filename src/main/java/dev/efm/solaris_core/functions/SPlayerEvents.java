package dev.efm.solaris_core.functions;

import dev.efm.solaris_core.common.capabilities.SolarisCapabilities;
import dev.efm.solaris_core.config.SolarisConfig;
import dev.efm.solaris_core.functions.backToDeath.PlayerPositionData;
import dev.efm.solaris_core.functions.resProc.IPlayerDurData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.List;

public class SPlayerEvents {
    private static final Capability<IPlayerDurData> DATA = SolarisCapabilities.PLAYER_DUR_DATA_CAPABILITY;

    @SubscribeEvent
    public static void PlayerClone(PlayerEvent.Clone evt) {
        ServerPlayer newPlayer = (ServerPlayer) evt.getEntity();
        ServerPlayer oldPlayer = (ServerPlayer) evt.getOriginal();
        if (newPlayer.level().isClientSide() || !evt.isWasDeath()) return;

        newPlayer.getCapability(DATA).ifPresent(playerData -> {
            playerData.setValue(SolarisConfig.RESPROC_TIME.get() * 20);
        });

        newPlayer.getCapability(SolarisCapabilities.PLAYER_D_LIST_CAP_CAPABILITY).ifPresent(data1 -> {
            oldPlayer.reviveCaps();
            oldPlayer.getCapability(SolarisCapabilities.PLAYER_D_LIST_CAP_CAPABILITY).ifPresent(data0 -> {
                List<PlayerPositionData> oldList = data0.getDList();
                List<PlayerPositionData> newList = oldList.stream().map(data -> new PlayerPositionData(data.position, data.dimension)).toList();
                data1.setDList(newList.size() > 10 ? newList.subList(0, 9) : newList);
            });
        });

        oldPlayer.discard();
    }

    @SubscribeEvent
    public static void PlayerLogin(PlayerEvent.PlayerLoggedInEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer player && !player.level().isClientSide) {
            player.getCapability(DATA).ifPresent(data -> {
                data.setValue(SolarisConfig.RESPROC_TIME.get() * 20);
            });
        }

        if (evt.getEntity() != null) {
            Player player = evt.getEntity();
            player.giveExperiencePoints(0);
        }
    }

    @SubscribeEvent
    public static void PlayerHurt(LivingDamageEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer player && SolarisConfig.ENABLE_RESPROC.get()) {

            player.getCapability(DATA).ifPresent(playerData -> {
                int data = playerData.getValue();

                if (data > 0) {
                    evt.setAmount(0f);
                }

            });

        }
    }

    @SubscribeEvent
    public static void PlayerDeath(LivingDeathEvent evt) {
        if (evt.getEntity() instanceof ServerPlayer player && !player.level().isClientSide) {
            player.getCapability(SolarisCapabilities.PLAYER_D_LIST_CAP_CAPABILITY).ifPresent(data -> {
                data.addData(new PlayerPositionData(player.position(), player.serverLevel().dimension().location()));
                player.sendSystemMessage(Component.translatable("tip.solaris_core.death_position").withStyle(ChatFormatting.RED).append(Component.literal(String.format("%s %.1f , %.1f , %.1f", player.serverLevel().dimension().location().toString(), player.position().x, player.position().y, player.position().z)).withStyle(ChatFormatting.YELLOW)));
            });
        }
    }

    @SubscribeEvent
    public static void PlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent evt) {
        if (evt.getEntity() != null) {
            Player player = evt.getEntity();
            player.giveExperiencePoints(1);
            player.giveExperiencePoints(-1);
        }
    }
}
