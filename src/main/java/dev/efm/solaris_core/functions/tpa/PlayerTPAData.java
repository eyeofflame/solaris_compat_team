package dev.efm.solaris_core.functions.tpa;

import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class PlayerTPAData {
    public final ServerPlayer reciver;
    public final ServerPlayer noticer;
    public final boolean mode;


    public PlayerTPAData(ServerPlayer reciver, ServerPlayer noticer, boolean mode) {
        this.reciver = reciver;
        this.noticer = noticer;
        this.mode = mode;
    }

    public static boolean compareSame(ServerPlayer reciver, ServerPlayer noticer, HashMap<UUID, PlayerTPAData> dataHashMap) {
        AtomicBoolean check = new AtomicBoolean(false);
        dataHashMap.forEach((uuid, playerTPAData) -> {
            if (playerTPAData.reciver.equals(reciver) && playerTPAData.noticer.equals(noticer)) check.set(true);
        });
        return check.get();
    }
}
