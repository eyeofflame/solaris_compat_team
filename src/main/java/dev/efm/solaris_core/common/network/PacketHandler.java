package dev.efm.solaris_core.common.network;

import dev.efm.solaris_core.SolarisCore;
import dev.efm.solaris_core.helper.GameHelper;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "11451";
    public static final SimpleChannel Instance = NetworkRegistry.newSimpleChannel(
            GameHelper.buildRes(SolarisCore.MODID, "double_jump"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static final SimpleChannel GUI_INSTANCE = NetworkRegistry.newSimpleChannel(
            GameHelper.buildRes(SolarisCore.MODID, "gui"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static final SimpleChannel GHOST_INSTANCE = NetworkRegistry.newSimpleChannel(
            GameHelper.buildRes(SolarisCore.MODID, "ghostfix"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;
    private static int GUI_Id = 0;
    private static int ghost_id = 0;

    public static void register() {
        Instance.registerMessage(
                packetId++,
                DoubleJumpPacket.class,
                DoubleJumpPacket::encode,
                DoubleJumpPacket::decode,
                DoubleJumpPacket::handle
        );

        GUI_INSTANCE.registerMessage(
                GUI_Id++,
                UIPacket.class,
                UIPacket::encode,
                UIPacket::decode,
                UIPacket::handle
        );

        GHOST_INSTANCE.registerMessage(ghost_id++,
                GhostBlockFixPacket.class,
                GhostBlockFixPacket::encode,
                GhostBlockFixPacket::decode,
                GhostBlockFixPacket::handle
        );
    }
}
