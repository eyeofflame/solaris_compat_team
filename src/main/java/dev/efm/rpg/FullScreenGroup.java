package dev.efm.rpg;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

public class FullScreenGroup extends WidgetGroup {

    private Consumer<FullScreenGroup> layout = g -> {
    };

    public FullScreenGroup() {
        super(0, 0, 0, 0);
    }

    public FullScreenGroup onLayout(Consumer<FullScreenGroup> layout) {
        this.layout = layout;
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onScreenSizeUpdate(int screenWidth, int screenHeight) {
        ModularUI gui = getGui();
        if (gui != null && gui.isFullScreen()) {
            setSize(screenWidth, screenHeight);
            layout.accept(this);
        }
        super.onScreenSizeUpdate(screenWidth, screenHeight);
    }
}
