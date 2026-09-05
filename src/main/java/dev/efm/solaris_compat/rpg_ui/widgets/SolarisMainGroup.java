package dev.efm.solaris_compat.rpg_ui.widgets;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.layout.Align;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

public class SolarisMainGroup extends WidgetGroup {
    private final float widthRatio;
    private final float heightRatio;
    private final int marginX;
    private final int marginY;
    private final Align corner;

    private final List<Component> textContent = new ArrayList<>();
    private ComponentPanelWidget textPanel;

    private long lastClick = 0;

    public SolarisMainGroup(float widthRatio, float heightRatio, int marginX, int marginY, Align corner) {
        super(0, 0, 1, 1);
        this.widthRatio = widthRatio;
        this.heightRatio = heightRatio;
        this.marginX = marginX;
        this.marginY = marginY;
        this.corner = corner;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onScreenSizeUpdate(int screenWidth, int screenHeight) {
        int w = (int) (screenWidth * widthRatio);
        int h = (int) (screenHeight * heightRatio);
        int x, y;
        switch (corner) {
            case TOP_LEFT -> {
                x = marginX;
                y = marginY;
            }
            case TOP_CENTER -> {
                x = (screenWidth - w) / 2;
                y = marginY;
            }
            case TOP_RIGHT -> {
                x = screenWidth - w - marginX;
                y = marginY;
            }
            case CENTER -> {
                x = (screenWidth - w) / 2;
                y = (screenHeight - h) / 2;
            }
            case BOTTOM_LEFT -> {
                x = marginX;
                y = screenHeight - h - marginY;
            }
            case BOTTOM_CENTER -> {
                x = (screenWidth - w) / 2;
                y = screenHeight - h - marginY;
            }
            case BOTTOM_RIGHT -> {
                x = screenWidth - w - marginX;
                y = screenHeight - h - marginY;
            }
            default -> {
                x = marginX;
                y = marginY;
            }
        }
        this.setSelfPosition(x, y);
        this.setSize(w, h);

        if (textPanel != null) {
            textPanel.setSelfPosition(5, 5);
            textPanel.setMaxWidthLimit(Math.max(1, w - 10));
        }

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
            Player player = this.getGui().entityPlayer;
            player.sendSystemMessage(Component.literal("hello!"));
        }
        super.handleClientAction(id, buffer);
    }


    public SolarisMainGroup setText(Component... lines) {
        textContent.clear();
        textContent.addAll(List.of(lines));
        if (textPanel == null) {
            textPanel = new ComponentPanelWidget(5, 5, list -> list.addAll(textContent));
            textPanel.setSpace(2);
            addWidget(textPanel);
        }
        return this;
    }
}
