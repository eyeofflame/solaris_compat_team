package dev.efm.solaris_compat.rpg_ui;

import com.lowdragmc.lowdraglib.networking.LDLNetworking;
import dev.efm.solaris_compat.rpg_ui.network.UIPacket;
import net.minecraft.world.entity.player.Player;

public interface RpgGuiApi {
    static void createTextGUI(Player player) {
        LDLNetworking.NETWORK.sendToServer(new UIPacket());
    }
}
