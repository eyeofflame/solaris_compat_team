package com.example.galgame.client;

import com.example.galgame.api.CharacterProfile;
import com.example.galgame.api.GalGameScript;
import com.lowdragmc.lowdraglib.gui.animation.Transform;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TextTextureWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.interpolate.Eases;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * GalGame 对话引擎：一个覆盖整个界面（逻辑分辨率 1280x720）的 {@link WidgetGroup}，
 * 负责渲染剧情层（背景、立绘、姓名、打字机正文、选项按钮）并驱动剧本状态机。
 *
 * <p>该组件完全基于 LDlib 的 GUI 体系实现：文本自动换行由 {@link TextTexture}
 * 的 {@link TextTexture.TextType#LEFT} 提供（见 LDlib 源码 {@code TextTexture#setWidth}），
 * 立绘滑入用 {@link Transform}，动态选项用 {@link WidgetGroup#addWidget} 的延迟同步列表。</p>
 */
@OnlyIn(Dist.CLIENT)
public final class GalGameDialogWidget extends WidgetGroup {

    /* ---------------- 逻辑分辨率与版式常量 ---------------- */
    public static final int UI_W = 1280;
    public static final int UI_H = 720;

    private static final int BOX_X = 36,  BOX_Y = 514,  BOX_W = 1208, BOX_H = 156;
    private static final int NAME_X = 60, NAME_Y = 524;
    private static final int TXT_X = 60,  TXT_Y = 548,  TXT_W = 1160, TXT_H = 110;
    private static final int P_X   = 60,  P_Y   = 120,  P_W   = 360,  P_H   = 400;
    private static final int CH_X  = 400, CH_Y  = 280,  CH_W  = 480;

    /** 打字机每秒揭示的码点数量。 */
    public static final float CHARS_PER_SECOND = 28.0f;
    /** 防跳转死循环的最大执行深度。 */
    private static final int MAX_EXECUTE_DEPTH = 200;

    /* ---------------- 渲染子组件 ---------------- */
    private final ImageWidget backgroundLayer;
    private final ImageWidget portraitLayer;
    private final WidgetGroup textBox;             // 半透明对话底框
    private final TextTextureWidget dialogue;      // 正文（自动换行 + 打字机）
    private final LabelWidget nameLabel;           // 说话者姓名
    private final WidgetGroup choiceContainer;     // 选项按钮容器

    /* ---------------- 角色注册表（宿主注入） ---------------- */
    private final Map<String, CharacterProfile> characters;

    /* ---------------- 剧本状态机 ---------------- */
    private GalGameScript.Step[] stepsArray;
    private int index;
    private boolean choiceActive;
    private String fullText = "";
    private long typeStartMs = -1;
    private int revealed = 0;
    @Nullable private final Runnable onFinished;

    /**
     * 构造对话引擎。
     *
     * @param characters 角色注册表（speakerId -&gt; 角色元数据）
     * @param onFinished 剧本结束回调（例如恢复玩家控制），可为 null
     */
    public GalGameDialogWidget(Map<String, CharacterProfile> characters,
                               @Nullable Runnable onFinished) {
        super(0, 0, UI_W, UI_H);
        this.characters = characters;
        this.onFinished = onFinished;

        // 界面最底层的纯色背景（不被剧本背景覆盖的区域）
        setBackground(new IGuiTexture[]{new ColorRectTexture(0xFF101820)});

        // 剧本背景层（最底）
        this.backgroundLayer = new ImageWidget(0, 0, UI_W, UI_H, IGuiTexture.EMPTY);
        addWidget(backgroundLayer);

        // 立绘层（左侧；默认隐藏，切换时用 Transform 滑入）
        this.portraitLayer = new ImageWidget(P_X, P_Y, P_W, P_H, IGuiTexture.EMPTY);
        portraitLayer.setVisible(false);
        addWidget(portraitLayer);

        // 底部对话底框
        this.textBox = new WidgetGroup(BOX_X, BOX_Y, BOX_W, BOX_H);
        textBox.setBackground(new IGuiTexture[]{
                new ColorRectTexture(0xCC0C1016).setTopRadius(6f).setBottomRadius(6f)});
        addWidget(textBox);

        // 姓名（金色，置于底框上方左侧）
        this.nameLabel = new LabelWidget(NAME_X, NAME_Y, () -> readName());
        nameLabel.setTextColor(0xFF_D7_BA_6B);
        addWidget(nameLabel);

        // 正文：左对齐 + 自动换行
        this.dialogue = new TextTextureWidget(TXT_X, TXT_Y, TXT_W, TXT_H);
        dialogue.textureStyle(t -> t.setWidth(TXT_W)
                .setColor(0xFFFFFFFF)
                .setDropShadow(true)
                .setType(TextTexture.TextType.LEFT));
        addWidget(dialogue);

        // 选项容器（居中，默认隐藏）
        this.choiceContainer = new WidgetGroup(CH_X, CH_Y, CH_W, 0);
        choiceContainer.setVisible(false);
        addWidget(choiceContainer);
    }

    /**
     * 载入一段剧本并从第 0 步开始演出。
     *
     * @param script 不可变剧本（通常由 {@link GalGameScript.Builder#build()} 得到）
     */
    public void load(GalGameScript script) {
        this.stepsArray = script.steps.toArray(new GalGameScript.Step[0]);
        this.index = -1;
        execute(0, 0);
    }

    /* ================== 剧本状态机 ================== */

    /** 执行指定索引的步骤；带深度上限防止跳转死循环。 */
    private void execute(int target, int depth) {
        if (depth > MAX_EXECUTE_DEPTH) { finish(); return; }
        if (target < 0 || target >= stepsArray.length) { finish(); return; }

        this.index = target;
        this.choiceActive = false;
        hideChoices();

        GalGameScript.Step s = stepsArray[target];
        if (s instanceof GalGameScript.SpeakStep sp) {
            this.fullText = sp.text.getString();
            this.revealed = 0;
            this.typeStartMs = System.currentTimeMillis();
            if (sp.speakerId != null && characters.containsKey(sp.speakerId)) {
                CharacterProfile p = characters.get(sp.speakerId);
                String pose = (sp.pose != null && p.hasPose(sp.pose)) ? sp.pose : "base";
                if (p.hasPose(pose)) {
                    portraitLayer.setImage(new ResourceTexture(p.texture(pose)));
                    portraitLayer.setVisible(true);
                } else {
                    portraitLayer.setVisible(false);
                }
            } else {
                portraitLayer.setVisible(false); // 旁白：隐藏立绘
            }
            refreshText();

        } else if (s instanceof GalGameScript.ShowPortraitStep sh) {
            CharacterProfile p = characters.get(sh.speakerId);
            ResourceLocation tex = p == null ? null : p.texture(sh.pose);
            if (tex != null) {
                portraitLayer.setImage(new ResourceTexture(tex));
                portraitLayer.setVisible(true);
                portraitLayer.animation(new Transform().offset(-90, 0).setIn().duration(550).ease(Eases.EaseQuadOut));
            }

        } else if (s instanceof GalGameScript.HidePortraitStep) {
            portraitLayer.setVisible(false);

        } else if (s instanceof GalGameScript.SetBackgroundStep bg) {
            backgroundLayer.setImage(new ResourceTexture(bg.background));

        } else if (s instanceof GalGameScript.ChoiceStep ch) {
            this.choiceActive = true;
            buildChoices(ch);

        } else if (s instanceof GalGameScript.JumpStep j) {
            execute(j.target, depth + 1);

        } else if (s instanceof GalGameScript.EndStep) {
            finish();
        }
    }

    /**
     * 玩家推进：文本未打完则立即补全；打完且当前不是选项则进入下一步；是选项则等待选择。
     */
    private void advance() {
        if (typeStartMs >= 0 && revealed < codepointCount(fullText)) {
            revealed = codepointCount(fullText);
            refreshText();
            typeStartMs = -1;
            return;
        }
        if (choiceActive) return;
        execute(index + 1, 0);
    }

    /** 结束演出：回调 + 关闭屏幕。 */
    private void finish() {
        if (onFinished != null) onFinished.run();
        if (gui != null && gui.getModularUIGui() != null) {
            Minecraft.getInstance().setScreen(null);
        }
    }

    /** 玩家选中某选项后跳转。 */
    private void selectChoice(GalGameScript.ChoiceOption option) {
        hideChoices();
        execute(option.target, 0);
    }

    /* ================== 选项按钮（动态构建） ================== */

    private void buildChoices(GalGameScript.ChoiceStep step) {
        choiceContainer.clearAllWidgets();
        int y = 0;
        for (GalGameScript.ChoiceOption opt : step.options) {
            final GalGameScript.ChoiceOption o = opt;
            choiceContainer.addWidget(makeChoiceButton(opt, y, () -> selectChoice(o)));
            y += 44;
        }
        choiceContainer.setSize(CH_W, y);
        choiceContainer.setVisible(true);
    }

    private ButtonWidget makeChoiceButton(GalGameScript.ChoiceOption opt, int y, Runnable onClick) {
        IGuiTexture base = new GuiTextureGroup(
                new ColorRectTexture(0xFF1B2530).setRadius(6f),
                makeChoiceText(opt));
        IGuiTexture hover = new GuiTextureGroup(
                new ColorRectTexture(0xFF2C3A4A).setRadius(6f),
                makeChoiceText(opt));
        return new ButtonWidget(0, y, CH_W, 34, base, cd -> onClick.run())
                .setHoverTexture(hover);
    }

    private static TextTexture makeChoiceText(GalGameScript.ChoiceOption opt) {
        return new TextTexture(opt.text.getString())
                .setWidth(CH_W).setColor(0xFFFFFFFF).setDropShadow(true);
    }

    private void hideChoices() {
        choiceContainer.clearAllWidgets();
        choiceContainer.setVisible(false);
    }

    /* ================== 打字机刷新 ================== */

    /** LDlib 会在客户端每帧自动调用（见 ModularUIGuiContainer 对 updateScreen 的转发）。 */
    @Override
    public void updateScreen() {
        super.updateScreen();
        long now = System.currentTimeMillis();
        if (typeStartMs >= 0) {
            int total = codepointCount(fullText);
            int target = Math.min(total,
                    (int) ((now - typeStartMs) / 1000.0f * CHARS_PER_SECOND));
            if (target != revealed) {
                revealed = target;
                refreshText();
                if (revealed >= total) typeStartMs = -1;
            }
        }
    }

    private void refreshText() {
        dialogue.setText(Component.literal(subByCodePoints(fullText, revealed)));
        nameLabel.setTextProvider(() -> readName());
    }

    /** 读取当前步骤说话者的本地化姓名（旁白返回空字符串）。 */
    private String readName() {
        GalGameScript.Step s = (stepsArray != null && index >= 0 && index < stepsArray.length)
                ? stepsArray[index] : null;
        if (s instanceof GalGameScript.SpeakStep sp && sp.speakerId != null) {
            CharacterProfile p = characters.get(sp.speakerId);
            if (p != null) return Component.translatable(p.displayNameKey).getString();
        }
        return "";
    }

    /* ================== 输入 ================== */

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) return true; // 选项按钮优先
        if (isVisible() && isMouseOverElement(mouseX, mouseY) && button == 0) {
            if (!choiceActive) advance();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 32 || keyCode == 257) {      // Space / Enter
            if (!choiceActive) advance();
            return true;
        }
        if (keyCode == 256) {                       // Esc
            Minecraft.getInstance().setScreen(null);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    /* ================== 工具方法 ================== */

    private static int codepointCount(String s) { return s.codePointCount(0, s.length()); }

    /** 按“码点”截取前缀，避免切开 CJK / emoji 的代理对。 */
    private static String subByCodePoints(String s, int count) {
        int[] cps = s.codePoints().toArray();
        if (count >= cps.length) return s;
        if (count <= 0) return "";
        int end = 0;
        for (int i = 0; i < count; i++) end += Character.charCount(cps[i]);
        return s.substring(0, end);
    }
}
