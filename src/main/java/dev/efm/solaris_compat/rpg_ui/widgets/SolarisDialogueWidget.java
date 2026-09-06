package dev.efm.solaris_compat.rpg_ui.widgets;

import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.ComponentPanelWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.layout.Align;
import dev.efm.solaris_compat.rpg_ui.data.DialogueNode;
import dev.efm.solaris_compat.rpg_ui.data.DialogueScript;
import dev.efm.solaris_compat.rpg_ui.session.DialogueSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SolarisDialogueWidget extends SolarisMainGroup {
    private DialogueSession session;
    private DialogueNode currentNode;
    private LabelWidget speakerLabel;
    private WidgetGroup choiceGroup;
    private ComponentPanelWidget textPanel;
    private static final int ACT_ADVANCE = 1;
    private static final int ACT_CHOICE = 2;
    private static final int ACT_CLOSE = 3;

    public SolarisDialogueWidget(float widthRatio, float heightRatio, int marginX, int marginY, Align corner) {
        super(widthRatio, heightRatio, marginX, marginY, corner);
        speakerLabel = new LabelWidget(10, 8, "");
        addWidget(speakerLabel);
        choiceGroup = new WidgetGroup();
        addWidget(choiceGroup);
    }

    public void setScript(DialogueScript script) {
        // 服务端：为这个打开会话建 session（attach 到 widget 的服务端实例）
        if (getGui() != null && getGui().holder != null
                && getGui().entityPlayer instanceof ServerPlayer sp) {
            session = DialogueSession.open(sp, script);
        }
        showNode(script.startNode());
    }

    /**
     * 显示一个节点：更新发言人 + 触发父类打字机。
     */
    private void showNode(DialogueNode node) {
        if (node == null) {
            closeUI();
            return;
        }
        currentNode = node;
        speakerLabel.setText(node.speaker() == null ? "" : node.speaker());
        setText(node.text());           // 复用父类：清 fullText、等打字/直接打字
        rebuildChoices(node);
    }

    /**
     * 分支：重建选项按钮；线性：清空。
     */
    private void rebuildChoices(DialogueNode node) {
        choiceGroup.clearAllWidgets();
        if (!node.hasChoices()) return;
        int y = 0;
        for (var choice : node.choices()) {
            final String nextId = choice.nextId();
            ButtonWidget btn = new ButtonWidget(0, y, 120, 16,
                    click -> {
                        // 服务端：点选项 → 校验并推进
                        if (!isRemoteSide()) {
                            DialogueNode n = session != null ? session.advance(nextId) : null;
                            if (n != null) pushNode(n);
                            else closeUI();
                        }
                    });
            //btn.setText(choice.text());// ButtonWidget 有 setText? —— 见下方说明
            btn.setButtonTexture(new TextTexture(choice.text()));
            choiceGroup.addWidget(btn);
            y += 18;
        }
    }

    // ---- 客户端交互 ----
    @Override
    @OnlyIn(Dist.CLIENT)
    public boolean mouseClicked(double mx, double my, int button) {
        // 先让选项按钮有机会消费
        if (super.mouseClicked(mx, my, button)) return true;
        if (isMouseOverElement(mx, my)) {
            if (isTyping()) {
                finishTypingNow();          // 打字中：点击=快进（纯本地，不发包）
                return true;
            }
            if (currentNode != null && !currentNode.hasChoices() && currentNode.nextId() != null) {
                writeClientAction(ACT_ADVANCE, buf -> {
                });   // 打完且线性：发推进
                return true;
            }
        }
        return false;
    }

    // ---- 服务端处理客户端动作 ----
    @Override
    public void handleClientAction(int id, FriendlyByteBuf buf) {
        switch (id) {
            case ACT_ADVANCE -> {
                if (session != null) {
                    DialogueNode n = session.advance(null);
                    if (n != null) pushNode(n);
                }
            }
            case ACT_CHOICE -> {
                String nextId = buf.readUtf();
                if (session != null) {
                    DialogueNode n = session.advance(nextId);
                    if (n != null) pushNode(n);
                }
            }
            case ACT_CLOSE -> closeUI();
            default -> super.handleClientAction(id, buf);
        }
    }

    /**
     * 服务端 → 客户端：把新节点推过去。
     */
    private void pushNode(DialogueNode node) {
        writeUpdateInfo(1, buffer -> buffer.writeNbt(node.serializeNBT()));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, FriendlyByteBuf buf) {
        if (id == 1) {
            showNode(DialogueNode.deserializeNBT(buf.readNbt()));
            return;
        }
        super.readUpdateInfo(id, buf);
    }

    private boolean isRemoteSide() {
        return getGui() == null || getGui().holder == null || getGui().holder.isRemote();
    }

    private void closeUI() {
        if (getGui() != null && getGui().getModularUIContainer() != null) {
            getGui().getModularUIContainer().removed(getGui().entityPlayer);
        }
    }
}
