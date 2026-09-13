package dev.efm.rpg.widgets;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.efm.rpg.data.Choice;

import java.util.List;
import java.util.function.Consumer;

/**
 * 分支选项容器。按钮在每次节点变化时整批重建。
 *
 * <p>重建只会在 {@link DialogueRoot#updateScreen()} 里被调用，那时 {@code super.updateScreen()}
 * 已经遍历完子控件，所以在自己的子列表上增删是安全的。
 * 按钮的点击回调也<b>不直接改控件</b>，只推进状态机——控件刷新交给下一次 tick 的轮询。
 */
public class ChoicePanel extends WidgetGroup {

    private static final int BUTTON_HEIGHT = 22;
    private static final int GAP = 6;

    private final Consumer<String> onChoose;
    private int buttonWidth = 200;

    public ChoicePanel(Consumer<String> onChoose) {
        super(0, 0, 0, 0);
        this.onChoose = onChoose;
    }

    public void setButtonWidth(int buttonWidth) {
        this.buttonWidth = Math.max(60, buttonWidth);
    }

    /** 按当前节点的选项重建按钮，传空列表就是清空。 */
    public void rebuild(List<Choice> choices) {
        clearAllWidgets();
        int y = 0;
        for (Choice choice : choices) {
            ButtonWidget button = new ButtonWidget(0, y, buttonWidth, BUTTON_HEIGHT,
                    new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON, new TextTexture(choice.text())),
                    clickData -> onChoose.accept(choice.id()));
            addWidget(button);
            y += BUTTON_HEIGHT + GAP;
        }
        setSize(buttonWidth, Math.max(0, y - GAP));
    }
}
