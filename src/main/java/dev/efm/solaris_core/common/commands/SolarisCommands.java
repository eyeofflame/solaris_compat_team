package dev.efm.solaris_core.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.efm.solaris_core.common.capabilities.SolarisCapabilities;
import dev.efm.solaris_core.common.network.GhostBlockFixPacket;
import dev.efm.solaris_core.common.network.PacketHandler;
import dev.efm.solaris_core.common.network.UIPacket;
import dev.efm.solaris_core.functions.backToDeath.PlayerPositionData;
import dev.efm.solaris_core.functions.tpa.PlayerTPAData;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class SolarisCommands {
    //true:to that , false:come here
    private static HashMap<UUID, PlayerTPAData> DATA_SET = new HashMap<UUID, PlayerTPAData>();

    public static final ScheduledExecutorService S_SERVICE = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    });

    public static void onRegisterCommands(RegisterCommandsEvent evt) {
        CommandDispatcher<CommandSourceStack> dispatcher = evt.getDispatcher();

        dispatcher.register(
                Commands.literal("back")
                        .executes(context -> {
                            if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                player.getCapability(SolarisCapabilities.PLAYER_D_LIST_CAP_CAPABILITY).ifPresent(cap -> {
                                    if (!cap.getDList().isEmpty()) {
                                        PlayerPositionData data = cap.getDList().get(0);
                                        ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, data.dimension);
                                        ServerLevel serverLevel = player.server.getLevel(resourceKey);
                                        player.teleportTo(serverLevel, data.position.x, data.position.y, data.position.z, player.getYRot(), player.getXRot());
                                        player.sendSystemMessage(Component.translatable("tip.solaris_core.death_command_sucess").append(String.format("%s %.1f , %.1f , %.1f", data.dimension.toString(), data.position.x, data.position.y, data.position.z)).withStyle(ChatFormatting.AQUA));
                                    } else {
                                        player.sendSystemMessage(Component.translatable("tip.solaris_core.death_command_failure").withStyle(ChatFormatting.RED));
                                    }
                                });
                            }
                            return 1;
                        })
                        .then(Commands.argument("id", IntegerArgumentType.integer(0, 10))
                                .executes(context -> {
                                    int id = IntegerArgumentType.getInteger(context, "id");

                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {

                                        player.getCapability(SolarisCapabilities.PLAYER_D_LIST_CAP_CAPABILITY).ifPresent(data -> {
                                            if ((data.getDList().size()) < id) {
                                                player.sendSystemMessage(Component.translatable("tip.solaris_core.death_command_failure").withStyle(ChatFormatting.RED));
                                            } else if ((data.getDList().size()) >= id) {
                                                PlayerPositionData positionData = data.getDList().get(id - 1);


                                                ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, positionData.dimension);
                                                ServerLevel serverLevel = player.server.getLevel(resourceKey);

                                                player.teleportTo(serverLevel, positionData.position.x, positionData.position.y, positionData.position.z, player.getYRot(), player.getXRot());
                                                player.sendSystemMessage(Component.translatable("tip.solaris_core.death_command_sucess").append(String.format("%s %.1f , %.1f , %.1f", positionData.dimension.toString(), positionData.position.x, positionData.position.y, positionData.position.z)).withStyle(ChatFormatting.AQUA));

                                            }

                                        });

                                    }

                                    return 1;
                                })
                        )
                        .then(Commands.literal("clear")
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        player.getCapability(SolarisCapabilities.PLAYER_D_LIST_CAP_CAPABILITY).ifPresent(data -> {
                                            player.sendSystemMessage(Component.translatable("tip.solaris_core.death_command_clear").withStyle(ChatFormatting.AQUA));
                                            System.out.println(data.getDList().size());
                                            data.clearData();
                                        });
                                    }
                                    return 1;
                                })
                        )
                        .then(Commands.literal("open")
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        player.getCapability(SolarisCapabilities.PLAYER_D_LIST_CAP_CAPABILITY).ifPresent(datalist -> {
                                            PacketHandler.GUI_INSTANCE.send(PacketDistributor.NMLIST.with(() -> Collections.singletonList(player.connection.connection)), new UIPacket(player.getId(), datalist.getDList(), -1f));
                                        });
                                    }
                                    return 1;
                                })
                        )
        );

        //tpa————————————————————————————————————————————————————

        dispatcher.register(
                Commands.literal("tpa")
                        .then(Commands.argument("targets", EntityArgument.player())
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer serverPlayer) {
                                        ServerPlayer player = EntityArgument.getPlayer(context, "targets");

                                        if (!PlayerTPAData.compareSame(player, serverPlayer, DATA_SET)) {
                                            UUID uuid = UUID.randomUUID();
                                            MutableComponent tpa = serverPlayer.getDisplayName().copy().withStyle(ChatFormatting.AQUA).append(Component.translatable("tip.solaris_core.tpa_0").withStyle(ChatFormatting.YELLOW));
                                            MutableComponent ACCEPT = Component.translatable("tip.solaris_core.tpa_accept").withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/tpaccept %s", uuid.toString()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("tip.solaris_core.tpa_accept_hover"))));
                                            MutableComponent DENY = Component.translatable("tip.solaris_core.tpa_deny").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/tpdeny %s", uuid.toString()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("tip.solaris_core.tpa_deny_hover"))));

                                            addData(player, serverPlayer, false, uuid);
                                            player.sendSystemMessage(tpa.append(ACCEPT).append(DENY));
                                        } else {
                                            serverPlayer.sendSystemMessage(Component.translatable("tip.solaris_core.tp_failure"));
                                        }

                                    }
                                    return 1;
                                })
                        )
        );

        dispatcher.register(
                Commands.literal("tpahere")
                        .then(Commands.argument("targets", EntityArgument.player())
                                .executes(context -> {
                                    ServerPlayer serverPlayer = context.getSource().getPlayer();
                                    ServerPlayer player = EntityArgument.getPlayer(context, "targets");

                                    if (!PlayerTPAData.compareSame(player, serverPlayer, DATA_SET) && serverPlayer != null) {
                                        UUID uuid = UUID.randomUUID();

                                        MutableComponent tpahere = serverPlayer.getDisplayName().copy().withStyle(ChatFormatting.AQUA).append(Component.translatable("tip.solaris_core.tpa_1").withStyle(ChatFormatting.YELLOW));
                                        MutableComponent ACCEPT = Component.translatable("tip.solaris_core.tpa_accept").withStyle(ChatFormatting.GREEN).withStyle(ChatFormatting.BOLD).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/tpaccept %s", uuid.toString()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("tip.solaris_core.tpa_accept_hover"))));
                                        MutableComponent DENY = Component.translatable("tip.solaris_core.tpa_deny").withStyle(ChatFormatting.RED).withStyle(ChatFormatting.BOLD).withStyle(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, String.format("/tpdeny %s", uuid.toString()))).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.translatable("tip.solaris_core.tpa_deny_hover"))));

                                        addData(player, serverPlayer, true, uuid);
                                        player.sendSystemMessage(tpahere.append(ACCEPT).append(DENY));
                                    } else {
                                        if (serverPlayer != null)
                                            serverPlayer.sendSystemMessage(Component.translatable("tip.solaris_core.tp_failure"));
                                    }

                                    return 1;
                                })
                        )
        );

        dispatcher.register(
                Commands.literal("tpaccept")
                        .then(Commands.argument("uuid", StringArgumentType.string())
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        UUID uuid = UUID.fromString(StringArgumentType.getString(context, "uuid"));
                                        if (DATA_SET.get(uuid) == null) {
                                            player.sendSystemMessage(Component.translatable("tip.solaris_core.tp_invalid").withStyle(ChatFormatting.RED));
                                        } else {
                                            PlayerTPAData data = DATA_SET.get(uuid);
                                            if (data.reciver.equals(player)) {
                                                ServerPlayer serverPlayer = data.noticer;
                                                if (data.mode) {
                                                    player.teleportTo(serverPlayer.serverLevel(), serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(), serverPlayer.getYRot(), serverPlayer.getXRot());
                                                    player.sendSystemMessage(Component.translatable("tip.solaris_core.tph_success").append(serverPlayer.getDisplayName().copy()).withStyle(ChatFormatting.AQUA));
                                                    serverPlayer.sendSystemMessage(Component.translatable("tip.solaris_core.tp_success").withStyle(ChatFormatting.AQUA));
                                                    DATA_SET.remove(uuid);
                                                } else {
                                                    serverPlayer.teleportTo(player.serverLevel(), player.getX(), player.getY(), player.getZ(), player.getYRot(), player.getXRot());
                                                    player.sendSystemMessage(serverPlayer.getDisplayName().copy().append(Component.translatable("tip.solaris_core.tpa_success")).withStyle(ChatFormatting.AQUA));
                                                    serverPlayer.sendSystemMessage(Component.translatable("tip.solaris_core.tp_success").withStyle(ChatFormatting.AQUA));
                                                    DATA_SET.remove(uuid);
                                                }
                                            }
                                        }
                                    }
                                    return 1;
                                })
                        )
        );

        dispatcher.register(
                Commands.literal("tpdeny")
                        .then(Commands.argument("uuid", StringArgumentType.string())
                                .executes(context -> {
                                    if (context.getSource().getEntity() instanceof ServerPlayer player) {
                                        UUID uuid = UUID.fromString(StringArgumentType.getString(context, "uuid"));
                                        if (DATA_SET.get(uuid) == null) {
                                            player.sendSystemMessage(Component.translatable("tip.solaris_core.tp_invalid").withStyle(ChatFormatting.RED));
                                        } else {
                                            PlayerTPAData data = DATA_SET.get(uuid);
                                            if (data.reciver.equals(player)) {
                                                ServerPlayer serverPlayer = data.noticer;
                                                player.sendSystemMessage(Component.translatable("tip.solaris_core.tp_deny_re").withStyle(ChatFormatting.YELLOW));
                                                serverPlayer.sendSystemMessage(player.getDisplayName().copy().append(Component.translatable("tip.solaris_core.tp_deny_no")).withStyle(ChatFormatting.YELLOW));
                                            }
                                        }
                                        DATA_SET.remove(uuid);
                                    }
                                    return 1;
                                })
                        )
        );

        dispatcher.register(
                Commands.literal("ghost")
                        .executes(ctx -> {
                            if (ctx.getSource().getEntity() instanceof Player player) {
                                PacketHandler.GHOST_INSTANCE.send(PacketDistributor.SERVER.noArg(), new GhostBlockFixPacket(player.blockPosition()));
                                player.sendSystemMessage(Component.translatable("tip.solaris_core.ghost_success").withStyle(ChatFormatting.AQUA));
                            }
                            return 1;
                        })
        );
    }

    private static void addData(ServerPlayer reciver, ServerPlayer noticer, boolean mode, UUID uuid) {
        DATA_SET.put(uuid, new PlayerTPAData(reciver, noticer, mode));

        S_SERVICE.schedule(() -> {
            if (DATA_SET.remove(uuid) != null) {
                reciver.sendSystemMessage(Component.translatable("tip.solaris_core.tpa_timeout").withStyle(ChatFormatting.YELLOW));
                noticer.sendSystemMessage(Component.translatable("tip.solaris_core.tpa_timeout").withStyle(ChatFormatting.YELLOW));
            }
        }, 30, TimeUnit.SECONDS);
    }
}
