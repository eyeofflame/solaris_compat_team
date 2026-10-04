package dev.efm.solaris_compat.ldlib;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import dev.efm.solaris_compat.data.TradeData;
import net.minecraft.world.entity.player.Player;

public record SolaTradeHolder(TradeData tradeData) implements IUIHolder {
    @Override
    public ModularUI createUI(Player entityPlayer) {
        return SolaTradeFactory.INSTANCE.createUITemplate(this, entityPlayer);
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
