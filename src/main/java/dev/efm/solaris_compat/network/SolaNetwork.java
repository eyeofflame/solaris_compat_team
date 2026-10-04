package dev.efm.solaris_compat.network;

import dev.efm.solaris_compat.SolarisCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 主 mod 的网络频道（此前没有任何网络代码）。
 *
 * <p>用 Forge 原生 {@link SimpleChannel}，不蹭 LDLib 的频道——那是别人家的，往里注册
 * 自己的包有消息 ID 冲突风险（与 solaris_rpg 的 RpgNetwork 同一理由）。
 * 协议版本对不上 Forge 会拒绝连接，改动包结构时记得一起提升 {@link #VERSION}。
 */
public final class SolaNetwork {

    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(SolarisCompat.MODID, "main"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private SolaNetwork() {
    }

    /** 在 {@code FMLCommonSetupEvent} 里调，两端都要。 */
    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CPacketOpenCoinPouch.class,
                CPacketOpenCoinPouch::encode,
                CPacketOpenCoinPouch::decode,
                CPacketOpenCoinPouch::handle);
    }

    /** 客户端 → 服务端：请求打开已装备的硬币袋。 */
    public static void sendOpenCoinPouch() {
        CHANNEL.sendToServer(new CPacketOpenCoinPouch());
    }
}
