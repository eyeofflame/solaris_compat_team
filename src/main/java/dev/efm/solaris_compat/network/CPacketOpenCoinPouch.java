package dev.efm.solaris_compat.network;

import dev.efm.solaris_compat.coin.CoinPouchService;
import dev.efm.solaris_compat.ldlib.CoinPouchHolder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：按快捷键请求打开硬币袋（只能是已装备在饰品槽里的那个）。
 *
 * <p>服务端复核玩家确实在硬币袋槽位装着硬币袋才开界面，防止伪造包。
 */
public class CPacketOpenCoinPouch {

    public void encode(FriendlyByteBuf buf) {
    }

    public static CPacketOpenCoinPouch decode(FriendlyByteBuf buf) {
        return new CPacketOpenCoinPouch();
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && CoinPouchService.hasEquipped(player)) {
                CoinPouchService.open(player, CoinPouchHolder.Source.CURIO);
            }
        });
        context.setPacketHandled(true);
    }
}
