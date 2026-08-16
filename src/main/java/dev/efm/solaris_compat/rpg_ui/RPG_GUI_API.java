package dev.efm.solaris_compat.rpg_ui;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import net.minecraft.world.entity.player.Player;

public interface RPG_GUI_API {
    default void createTextGUI(Player player) {
        ModularUI ui = new ModularUI(IUIHolder.EMPTY, player);
    }
}
