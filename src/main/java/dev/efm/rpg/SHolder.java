package dev.efm.rpg;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import dev.efm.rpg.data.Script;
import net.minecraft.world.entity.player.Player;

public record SHolder(Script script) implements IUIHolder {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        return SFactory.INSTANCE.createUITemplate(this, entityPlayer);
    }

    @Override
    public boolean isInvalid() {
        return false;
    }

    @Override
    public boolean isRemote() {
        return LDLib.isRemote();
    }

    @Override
    public void markAsDirty() {

    }
}
