package dev.efm.solaris_core.functions.backToDeath.menuHelper;

import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.util.ClickData;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class SolarisButtonWidget extends ButtonWidget {

    public SolarisButtonWidget() {
        super();
    }

    public SolarisButtonWidget(int x, int y, int w, int h,
                               IGuiTexture texture, Consumer<ClickData> onPressed) {
        super(x, y, w, h, texture, onPressed);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected void drawBackgroundTexture(@NotNull GuiGraphics graphics,
                                         int mouseX, int mouseY) {
        boolean isHovered = this.isMouseOverElement(mouseX, mouseY);

        // ① 背景/点击贴图
        if (!isHovered || this.drawBackgroundWhenHover) {
            if (this.isClicked && this.clickedTexture != null) {
                Position pos = this.getPosition();
                Size size = this.getSize();
                this.clickedTexture.draw(graphics, mouseX, mouseY,
                        (float) pos.x, (float) pos.y, size.width, size.height);
            } else if (this.backgroundTexture != null) {
                Position pos = this.getPosition();
                Size size = this.getSize();
                this.backgroundTexture.draw(graphics, mouseX, mouseY,
                        (float) pos.x, (float) pos.y, size.width, size.height);
            }
        }

        // ② Hover贴图 — 增加 !isClicked 判断！
        if (this.hoverTexture != null && isHovered
                && this.isActive() && !this.isClicked) {   // ← 加了这行
            Position pos = this.getPosition();
            Size size = this.getSize();
            this.hoverTexture.draw(graphics, mouseX, mouseY,
                    (float) pos.x, (float) pos.y, size.width, size.height);
        }
    }
}
