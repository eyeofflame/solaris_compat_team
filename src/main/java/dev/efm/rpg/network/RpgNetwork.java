package dev.efm.rpg.network;

import dev.efm.solaris_compat.SolarisCompat;
import dev.efm.solaris_compat.api.SHelper;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 对话系统的网络频道。
 *
 * <p>用 Forge 原生 {@link SimpleChannel}，不蹭 LDLib 的频道——那是别人家的，
 * 往里注册自己的包有消息 ID 冲突的风险。
 */
public final class RpgNetwork {

    /**
     * 协议版本。客户端和服务端对不上时 Forge 会直接拒绝连接，所以改动包结构时记得一起改。
     */
    private static final String VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            SHelper.buildRes(SolarisCompat.MODID, "rpg"),
            () -> VERSION,
            VERSION::equals,
            VERSION::equals);

    private RpgNetwork() {
    }

    /** 在 {@code FMLCommonSetupEvent} 里调，两端都要。 */
    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(id++, CPacketChoiceSelected.class,
                CPacketChoiceSelected::encode,
                CPacketChoiceSelected::decode,
                CPacketChoiceSelected::handle);
    }

    /** 客户端 → 服务端：告诉服务端玩家选了哪个选项。 */
    public static void sendChoice(String scriptId, String nodeId, String choiceId) {
        CHANNEL.sendToServer(new CPacketChoiceSelected(scriptId, nodeId, choiceId));
    }
}
