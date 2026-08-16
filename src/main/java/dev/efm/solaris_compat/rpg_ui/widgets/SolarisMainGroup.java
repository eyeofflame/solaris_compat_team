package dev.efm.solaris_compat.rpg_ui.widgets;

import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SolarisMainGroup extends WidgetGroup {
    private long lastClick = 0;

    public SolarisMainGroup(Position position, Size size) {
        super(position, size);
    }

    @Override
    public void onScreenSizeUpdate(int screenWidth, int screenHeight) {
        super.onScreenSizeUpdate(screenWidth, screenHeight);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean consumed = super.mouseClicked(mouseX, mouseY, button);
        long now = System.currentTimeMillis();
        if (now - lastClick >= 200) {
            lastClick = now;
            writeClientAction(11451, buf -> buf.writeVarInt(button));
        }
        return consumed;
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        if (id == 11451) {
            int button = buffer.readVarInt();

        }
        super.handleClientAction(id, buffer);
    }
}
