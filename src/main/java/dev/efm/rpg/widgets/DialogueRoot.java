package dev.efm.rpg.widgets;

import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ColorRectTexture;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceTexture;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.efm.rpg.StateEngine;
import dev.efm.rpg.data.Script;
import dev.efm.rpg.network.RpgNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

/**
 * Galgame 风格对话界面：全屏背景 + 角色立绘 + 底部对话框 + 上方分支选项。
 *
 * <p>结构（按绘制顺序，后面的盖在前面的上面）：
 * <pre>
 * DialogueRoot (屏幕大小, client-side)
 * ├─ background   ImageWidget          全屏背景
 * ├─ portrait     ImageWidget          角色立绘，对话框上方，可缺省
 * ├─ box          WidgetGroup          底部对话框
 * │   ├─ speakerLabel  LabelWidget     说话人，为空时隐藏
 * │   └─ text          TypewriterTextWidget
 * └─ choices      ChoicePanel          对话框上方的分支选项
 * </pre>
 *
 * <p>刷新方式是<b>轮询</b>而不是回调：{@link StateEngine} 只暴露一个 {@code revision} 计数器，
 * 这里每个客户端 tick 比对一次。这样按钮回调里不需要动控件，避开了
 * {@code WidgetGroup.mouseClicked} 遍历子控件时增删同层导致的 ConcurrentModificationException。
 *
 * <p>整棵子树标了 client-side，不进初始数据同步、也不会走 LDLib 的 client action 通道。
 * 对话推进完全在本地跑；唯一的网络流量是玩家点选项时自己发的一个 C2S 包
 * （见 {@link dev.efm.rpg.network.RpgNetwork}），用来让服务端触发脚本回调。
 *
 * <p><b>注意这里没有任何 {@code @OnlyIn(Dist.CLIENT)}。</b>构造函数里用到了 {@code this::layout}
 * 和 {@code this::onChoose} 两个方法引用，而 Forge 的 RuntimeDistCleaner 会在专用服务器上
 * 剥离带 {@code @OnlyIn(CLIENT)} 的方法 → 构造时就 NoSuchMethodError。这些方法本来就只在客户端
 * 被调用（控件只在客户端 tick/绘制），不需要靠注解来保证。
 */
public class DialogueRoot extends FullScreenGroup {

    private static final int BOX_MARGIN = 14;
    private static final int BOX_PADDING = 14;
    private static final int SPEAKER_BASELINE = 10;
    private static final int TEXT_BASELINE = 32;

    private final StateEngine engine;
    /** 发包回服务端时用来标识是哪个剧本。 */
    private final String scriptId;
    private int lastRevision = -1;
    private boolean pendingClose;

    /** 立绘高度占屏幕高度的比例。 */
    private float portraitHeightRatio = 0.62f;
    /** 立绘水平锚点：0 = 贴左边，0.5 = 居中，1 = 贴右边。 */
    private float portraitAnchor = 0.5f;
    /** 上一帧用的立绘路径，避免每次布局都重建 ResourceTexture。 */
    private String lastPortraitPath = "";

    private final ImageWidget background;
    private final ImageWidget portrait;
    private final WidgetGroup box;
    private final LabelWidget speakerLabel;
    private final TypewriterTextWidget text;
    private final ChoicePanel choices;

    public DialogueRoot(Script script) {
        engine = new StateEngine(script);
        engine.start();
        scriptId = script == null ? "" : script.scriptId();

        background = new ImageWidget(0, 0, 0, 0,
                new ResourceBorderTexture("solaris_compat:textures/gui/sola_background.png", 16, 16, 5, 5));

        portrait = new ImageWidget(0, 0, 0, 0, IGuiTexture.EMPTY);
        portrait.setVisible(false);

        speakerLabel = new LabelWidget(0, 0, "");
        speakerLabel.setTextColor(0xFFD9A441);
        speakerLabel.setDropShadow(true);

        text = new TypewriterTextWidget(0, 0, 0, 0);

        choices = new ChoicePanel(this::onChoose);

        box = new WidgetGroup(0, 0, 0, 0);
        box.setBackground(new ColorRectTexture(0xC8101018), new ColorBorderTexture(1, 0xFF6A7A9A));
        box.addWidget(speakerLabel);
        box.addWidget(text);

        addWidget(background);
        addWidget(portrait);   // 背景之上、对话框之下
        addWidget(box);
        addWidget(choices);

        onLayout(this::layout);

        // 放在最后：会把 client-side 标记递归传给上面所有子控件
        setClientSideWidget();
    }

    // ------------------------------------------------------------------ 布局

    private void layout(FullScreenGroup root) {
        int screenWidth = root.getSizeWidth();
        int screenHeight = root.getSizeHeight();

        background.setSelfPosition(0, 0);
        background.setSize(screenWidth, screenHeight);

        // 对话框：屏幕宽度的 86%，但不超过 16:9；高度取屏幕高度的 28%，最小 72px
        int boxWidth = Math.min((int) (screenWidth * 0.86f), (int) (screenHeight * 16f / 9f));
        int boxHeight = Math.max(72, (int) (screenHeight * 0.28f));
        box.setSize(boxWidth, boxHeight);
        box.setSelfPosition((screenWidth - boxWidth) / 2, screenHeight - boxHeight - BOX_MARGIN);

        speakerLabel.setSelfPosition(BOX_PADDING, SPEAKER_BASELINE);
        text.setSelfPosition(BOX_PADDING, TEXT_BASELINE);
        text.setSize(boxWidth - BOX_PADDING * 2, boxHeight - TEXT_BASELINE - BOX_PADDING);

        choices.setButtonWidth(Math.min((int) (screenWidth * 0.4f), 320));

        syncToEngine(false);
    }

    // ------------------------------------------------------------------ 状态机 → 界面

    /**
     * @param restartText true 时强制重新逐字。只有节点真的换了才传 true；
     *                    窗口缩放走 false，否则拖一下窗口就要重新打一遍字。
     */
    private void syncToEngine(boolean restartText) {
        lastRevision = engine.revision();

        String speaker = engine.speaker();
        speakerLabel.setVisible(!speaker.isEmpty());
        speakerLabel.setText(speaker);

        text.setText(engine.text(), restartText);

        choices.rebuild(engine.choices());
        positionChoices();
        layoutPortrait();
    }

    private void positionChoices() {
        choices.setSelfPosition(
                (getSizeWidth() - choices.getSizeWidth()) / 2,
                box.getSelfPositionY() - choices.getSizeHeight() - 10);
    }

    // ------------------------------------------------------------------ 立绘

    /** 立绘高度占屏幕高度的比例。默认 0.62。 */
    public DialogueRoot setPortraitHeightRatio(float ratio) {
        this.portraitHeightRatio = Math.min(1.0f, Math.max(0.05f, ratio));
        return this;
    }

    /** 立绘水平锚点：0 = 贴左边，0.5 = 居中（默认），1 = 贴右边。 */
    public DialogueRoot setPortraitAnchor(float anchor) {
        this.portraitAnchor = Math.min(1.0f, Math.max(0.0f, anchor));
        return this;
    }

    /**
     * 把当前立绘摆到对话框正上方。
     *
     * <p>宽高比必须自己算：LDLib 的 {@code ResourceTexture} 是把整张图 UV 0..1 铺满控件矩形的，
     * 矩形比例不对立绘就会被拉伸。所以这里读贴图的真实像素尺寸，固定高度、反推宽度。
     *
     * <p>资源包没提供这张图时（{@link PortraitTextures#sizeOf} 返回 null）直接隐藏，不报错。
     */
    private void layoutPortrait() {
        String path = engine.portrait();
        if (path.isEmpty()) {
            portrait.setVisible(false);
            return;
        }
        ResourceLocation location = ResourceLocation.tryParse(path);
        int[] size = location == null ? null : PortraitTextures.sizeOf(location);
        if (size == null) {
            portrait.setVisible(false);
            return;
        }

        if (!path.equals(lastPortraitPath)) {
            lastPortraitPath = path;
            portrait.setImage(new ResourceTexture(path));
        }

        int height = Math.max(1, (int) (getSizeHeight() * portraitHeightRatio));
        int width = Math.max(1, (int) ((long) height * size[0] / size[1]));
        portrait.setSize(width, height);
        // 底边贴着对话框上沿
        portrait.setSelfPosition(
                (int) (getSizeWidth() * portraitAnchor - width / 2f),
                box.getSelfPositionY() - height);
        portrait.setVisible(true);
    }

    @Override
    public void updateScreen() {
        // 先让子控件 tick 完，之后动子控件列表才是安全的
        super.updateScreen();
        if (pendingClose) {
            pendingClose = false;
            closeDialogue();
            return;
        }
        if (engine.revision() != lastRevision) {
            syncToEngine(true);
        }
    }

    // ------------------------------------------------------------------ 输入

    /**
     * 点击选项按钮。只推进状态机，界面刷新交给下一次 tick 的轮询。
     *
     * <p>除了本地推进，还会发个包通知服务端"玩家选了什么"。顺序是先本地后发包：
     * 服务端的回调只是通知语义、不参与走向，所以包丢了也不会卡住对话。
     */
    private void onChoose(String choiceId) {
        String nodeId = engine.currentNodeId();
        if (engine.select(choiceId)) {
            RpgNetwork.sendChoice(scriptId, nodeId, choiceId);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 选项按钮先吃掉点击；没有子控件处理才轮到推进对话
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        advanceInput();
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_SPACE
                || keyCode == GLFW.GLFW_KEY_ENTER
                || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            advanceInput();
            return true;
        }
        return false;
    }

    /**
     * 推进对话，优先级：
     * <ol>
     *     <li>打字机还没放完 → 立刻显示全文（第一次点击是"跳过"）</li>
     *     <li>当前是分支节点 → 什么都不做，必须点选项</li>
     *     <li>否则前进；没有下一个节点就关界面</li>
     * </ol>
     */
    private void advanceInput() {
        if (!text.isFullyRevealed()) {
            text.revealAll();
            return;
        }
        if (engine.hasChoices()) {
            return;
        }
        if (!engine.advance()) {
            // 延到下一次 tick 再关：现在还在 mouseClicked / keyPressed 的调用栈里，
            // 直接 setScreen(null) 会在遍历控件的途中把界面拆掉
            pendingClose = true;
        }
    }

    private void closeDialogue() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }
}
