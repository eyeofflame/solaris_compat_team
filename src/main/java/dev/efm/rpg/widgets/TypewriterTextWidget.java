package dev.efm.rpg.widgets;

import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * 逐字显示的文本控件。LDLib 没有现成的打字机，这个是手写的。
 *
 * <p>关键点：<b>折行按全文算一次</b>，绘制时再按已显示字数逐行裁剪。
 * 如果反过来——把不断增长的前缀喂给一个会自动折行的控件——中文每多一个字都可能重新折行，
 * 文字会来回抖动。
 *
 * <p>只支持纯文本，不支持逐字样式。构造时就标了 client-side：不参与初始数据同步，
 * 也不会发 client action。
 *
 * <p><b>没有任何 {@code @OnlyIn(Dist.CLIENT)}。</b>客户端专属的行为靠运行时的
 * {@link #isRemote()} 判断来兜底，而不是靠 Forge 的 dist cleaner 剥离方法——
 * 后者会留下"方法被删掉但调用点还在"的 NoSuchMethodError。
 */
public class TypewriterTextWidget extends Widget {

    private static final int LINE_SPACING = 2;

    private String fullText = "";
    private List<String> lines = List.of();
    private int totalChars;
    private int revealed;

    private double charsPerSecond = 35.0;
    private boolean instant;
    private double charAccumulator;
    private long lastUpdateMs;

    private int color = 0xFFFFFF;
    private boolean dropShadow = true;

    public TypewriterTextWidget(int x, int y, int width, int height) {
        super(x, y, width, height);
        setClientSideWidget();
    }

    // ------------------------------------------------------------------ 外部 API

    public TypewriterTextWidget setText(String text) {
        return setText(text, false);
    }

    /**
     * 换一段文本。
     *
     * @param restart true 时即使文本和上次一样也从头逐字显示（切到内容相同的节点时用）
     */
    public TypewriterTextWidget setText(String text, boolean restart) {
        String normalized = text == null ? "" : text;
        if (!restart && normalized.equals(fullText)) {
            return this;
        }
        fullText = normalized;
        rewrap();
        revealed = instant ? totalChars : 0;
        charAccumulator = 0;
        lastUpdateMs = 0;
        return this;
    }

    public TypewriterTextWidget setCharsPerSecond(double charsPerSecond) {
        this.charsPerSecond = Math.max(1.0, charsPerSecond);
        return this;
    }

    /** true = 不做逐字，直接整段显示。 */
    public TypewriterTextWidget setInstant(boolean instant) {
        this.instant = instant;
        if (instant) {
            revealed = totalChars;
        }
        return this;
    }

    public TypewriterTextWidget setTextColor(int color) {
        this.color = color;
        return this;
    }

    public TypewriterTextWidget setTextDropShadow(boolean dropShadow) {
        this.dropShadow = dropShadow;
        return this;
    }

    public boolean isFullyRevealed() {
        return revealed >= totalChars;
    }

    /** 点击跳过：立刻显示全文。 */
    public void revealAll() {
        revealed = totalChars;
    }

    // ------------------------------------------------------------------ 生命周期

    @Override
    protected void onSizeUpdate() {
        super.onSizeUpdate();
        rewrap();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (instant || isFullyRevealed()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (lastUpdateMs == 0L) {
            lastUpdateMs = now;
            return;
        }
        double delta = (now - lastUpdateMs) / 1000.0;
        lastUpdateMs = now;
        if (delta <= 0) {
            return;
        }
        // 卡顿或者切出窗口再回来时，不要一次把整段喷出来
        delta = Math.min(delta, 0.5);
        charAccumulator += delta * charsPerSecond;
        int add = (int) charAccumulator;
        if (add > 0) {
            charAccumulator -= add;
            revealed = Math.min(revealed + add, totalChars);
        }
    }

    @Override
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        if (!isRemote() || lines.isEmpty()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        Position pos = getPosition();
        int remaining = revealed;
        for (int i = 0; i < lines.size() && remaining > 0; i++) {
            String line = lines.get(i);
            String toDraw = remaining >= line.length() ? line : line.substring(0, remaining);
            graphics.drawString(font, toDraw, pos.x, pos.y + i * (font.lineHeight + LINE_SPACING),
                    color, dropShadow);
            remaining -= line.length();
        }
    }

    // ------------------------------------------------------------------ 折行

    /**
     * 按当前宽度把 {@link #fullText} 折成若干行，只保留纯文本。
     * 服务端没有 Font（也没有渲染），靠 {@link #isRemote()} 直接跳过。
     */
    private void rewrap() {
        if (!isRemote()) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        int maxWidth = Math.max(1, getSizeWidth());
        List<String> result = new ArrayList<>();
        int total = 0;

        for (String paragraph : fullText.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                result.add("");
                continue;
            }
            int start = 0;
            while (start < paragraph.length()) {
                // 这一行最多能放下多少个字符
                int end = start;
                while (end < paragraph.length()
                        && font.width(paragraph.substring(start, end + 1)) <= maxWidth) {
                    end++;
                }
                if (end >= paragraph.length()) {
                    result.add(paragraph.substring(start));
                    break;
                }
                if (end == start) {
                    // 单个字符就超宽（宽度被挤没了），硬放一个，否则死循环
                    end = start + 1;
                } else {
                    // 行内有空格就退到最后一个空格处断，别把英文单词劈开
                    for (int i = end - 1; i > start; i--) {
                        if (paragraph.charAt(i) == ' ') {
                            end = i + 1;
                            break;
                        }
                    }
                }
                result.add(paragraph.substring(start, end));
                start = end;
            }
        }

        for (String line : result) {
            total += line.length();
        }
        lines = result;
        totalChars = total;
        if (revealed > totalChars) {
            revealed = totalChars;
        }
    }
}
