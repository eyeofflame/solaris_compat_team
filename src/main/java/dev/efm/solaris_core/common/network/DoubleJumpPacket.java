package dev.efm.solaris_core.common.network;

import dev.efm.solaris_core.common.effects.SMobEffectsRegister;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.Collections;
import java.util.function.Supplier;

public class DoubleJumpPacket {
    private final boolean isOnGround;
    private final int name;

    public DoubleJumpPacket(boolean isonGround, int name) {
        this.isOnGround = isonGround;
        this.name = name;
    }

    public static void encode(DoubleJumpPacket packet, FriendlyByteBuf buf) {
        buf.writeBoolean(packet.isOnGround);
        buf.writeInt(packet.name);
    }

    public static DoubleJumpPacket decode(FriendlyByteBuf buf) {
        return new DoubleJumpPacket(buf.readBoolean(), buf.readInt());
    }

    public static void handle(DoubleJumpPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = context.getSender();
                if (player == null) {
                    return;
                }

                if (!packet.isOnGround && player.getId() == packet.name) {
                    var check = player.getEffect(SMobEffectsRegister.DOUBLE_JUMP.get());
                    if (check != null && !player.getTags().contains("efm:jumped")) {
                        PacketHandler.Instance.send(PacketDistributor.NMLIST.with(() -> Collections.singletonList(player.connection.connection)), new DoubleJumpPacket(packet.isOnGround, packet.name));
                        player.addTag("efm:jumped");
                    }
                }
            } else if (context.getDirection().getReceptionSide().isClient()) {
                Minecraft mc = Minecraft.getInstance();
                var player = mc.player;
                if (player != null) {
                    player.jumpFromGround();
                }
            }

        });
        context.setPacketHandled(true);
    }
}
