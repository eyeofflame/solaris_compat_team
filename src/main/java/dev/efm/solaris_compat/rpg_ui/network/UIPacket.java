package dev.efm.solaris_compat.rpg_ui.network;

import com.lowdragmc.lowdraglib.networking.IHandlerContext;
import com.lowdragmc.lowdraglib.networking.IPacket;
import dev.efm.solaris_compat.rpg_ui.SolarisUIFactory;
import net.minecraft.network.FriendlyByteBuf;

public class UIPacket implements IPacket {
    public UIPacket() {
    }

    @Override
    public void encode(FriendlyByteBuf friendlyByteBuf) {

    }

    @Override
    public void decode(FriendlyByteBuf friendlyByteBuf) {

    }

    @Override
    public void execute(IHandlerContext handler) {
        if (handler.getPlayer() != null) {
            SolarisUIFactory.INSTANCE.openUI(new SolarisUIFactory.Holder(), handler.getPlayer());
        }
    }
}
