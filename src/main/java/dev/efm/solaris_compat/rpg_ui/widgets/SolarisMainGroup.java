package dev.efm.solaris_compat.rpg_ui.widgets;

import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.layout.Align;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

public class SolarisMainGroup extends WidgetGroup {
    private static final int CLICK_ACTION_ID = 11451;

    private final float widthRatio;
    private final float heightRatio;
    private final int marginX;
    private final int marginY;
    private final Align corner;

    private final List<Component> textContent = new ArrayList<>();
    private final List<Component> fullText = new ArrayList<>();
    private ComponentPanelWidget textPanel;

    private int progress = 0;
    private boolean running = false;

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
        if (!consumed && isMouseOverElement(mouseX, mouseY)) {
            long now = System.currentTimeMillis();
            if (now - lastClick >= 100) {
                lastClick = now;
                writeClientAction(CLICK_ACTION_ID, buf -> buf.writeVarInt(button));
            }
        }
        return consumed;
    }

    @Override
    public void handleClientAction(int id, FriendlyByteBuf buffer) {
        if (id == CLICK_ACTION_ID) {
            int button = buffer.readVarInt();
            Player player = this.getGui() == null ? null : this.getGui().entityPlayer;
            if (player != null) {
                player.sendSystemMessage(Component.literal("hello!"));
            }
            return;
        }
        super.handleClientAction(id, buffer);
    }


    public SolarisMainGroup setText(Component... lines) {
        textContent.clear();
        textContent.addAll(List.of(lines));
        fullText.clear();
        fullText.addAll(List.of(lines));
        running = false;
        progress = 0;
        if (textPanel == null) {
            textPanel = new ComponentPanelWidget(5, 5, list -> list.addAll(textContent));
            textPanel.setSpace(2);
            textPanel.setClientSideWidget();
            addWidget(textPanel);
        }
        rebuildText();
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void updateScreen() {
        super.updateScreen();
        if (running) {
            int total = fullText.stream().mapToInt(c -> c.getString().length()).sum();
            if (progress < total) {
                progress++;
                rebuildText();
            } else {
                running = false;
            }
        }
    }

    private void rebuildText() {
        textContent.clear();
        int remaining = progress;
        for (Component line : fullText) {
            if (remaining <= 0) break;
            Component slice = truncatePreservingStyle(line, remaining);
            textContent.add(slice);
            remaining -= line.getString().length();
        }
    }

    public void startType() {
        running = true;
    }

    private void flatten(Component component, Style style, List<Component> leaves) {
        Style s = component.getStyle().applyTo(style);
        if (component.getContents() instanceof LiteralContents lit) {
            String text = lit.text();
            if (!text.isEmpty()) {
                leaves.add(Component.literal(text).withStyle(s));
            }
        } else {
            String text = component.getString();
            if (!text.isEmpty()) {
                leaves.add(Component.literal(text).withStyle(s));
            }
        }
        for (Component sibling : component.getSiblings()) {
            flatten(sibling, s, leaves);
        }
    }

    private Component truncatePreservingStyle(Component line, int maxChars) {
        List<Component> leaves = new ArrayList<>();
        flatten(line, Style.EMPTY, leaves);
        MutableComponent result = Component.empty();
        int remaining = maxChars;
        for (Component leaf : leaves) {
            if (remaining <= 0) break;
            String s = leaf.getString();
            if (s.length() <= remaining) {
                result.append(leaf);
                remaining -= s.length();
            } else {
                result.append(Component.literal(s.substring(0, remaining)).withStyle(leaf.getStyle()));
                remaining = 0;
            }
        }
        return result;
    }

    public boolean isTyping() {
        return running;
    }

    public void finishTypingNow() {
        progress = fullText.stream().mapToInt(c -> c.getString().length()).sum();
        running = false;
        rebuildText();
    }

    public boolean hasNext() {
        return false;
    }
}
