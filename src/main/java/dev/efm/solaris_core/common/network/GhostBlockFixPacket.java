package dev.efm.solaris_core.common.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class GhostBlockFixPacket {
    private final BlockPos playerPos;

    public GhostBlockFixPacket(BlockPos playerPos) {
        this.playerPos = playerPos;
    }

    public static void encode(GhostBlockFixPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.playerPos);
    }

    public static GhostBlockFixPacket decode(FriendlyByteBuf buf) {
        return new GhostBlockFixPacket(buf.readBlockPos());
    }

    public static void handle(GhostBlockFixPacket packet, Supplier<NetworkEvent.Context> context) {
        NetworkEvent.Context ctx = context.get();
        ctx.enqueueWork(() -> {
            if (ctx.getDirection().getReceptionSide().isServer()) {
                ServerPlayer player = ctx.getSender();
                if (player != null) {
                    ServerLevel serverLevel = player.serverLevel();

                    int radius = 9;
                    BlockPos.betweenClosed(
                            packet.playerPos.offset(-radius, -radius, -radius),
                            packet.playerPos.offset(radius, radius, radius)
                    ).forEach(pos -> {
                        BlockState state = serverLevel.getBlockState(pos);
                        serverLevel.sendBlockUpdated(pos,state,state, Block.UPDATE_CLIENTS);
                        BlockEntity be = serverLevel.getBlockEntity(pos);
                        if (be != null){
                            Packet<?> packet1 = be.getUpdatePacket();
                            if (packet1 != null){
                                player.connection.send(packet1);
                            }
                        }
                    });
                }
            }
            ctx.setPacketHandled(true);
        });
    }
}
