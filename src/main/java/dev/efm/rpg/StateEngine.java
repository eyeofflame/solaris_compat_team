package dev.efm.rpg;

import dev.efm.rpg.data.Choice;
import dev.efm.rpg.data.Node;
import dev.efm.rpg.data.Script;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话状态机：一个按 id 查表的节点图遍历器。
 *
 * <p>纯逻辑，不依赖任何 Minecraft 类，两端都能安全构造。按当前设计只有客户端会推进它
 * ——整个 {@link Script} 已经随 holder 下发到客户端，而 {@link Choice} 不带任何服务端副作用，
 * 所以对话推进可以完全本地完成，零网络往返。
 *
 * <p>界面<b>不订阅回调</b>，而是每个客户端 tick 比对 {@link #revision()}。
 * 这样避免了一个坑：{@code WidgetGroup.mouseClicked} 是直接遍历 {@code widgets} 的，
 * 如果在按钮回调里增删同层控件会抛 ConcurrentModificationException。
 */
public class StateEngine {

    private final Script script;
    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private Node current;
    private boolean finished;
    private int revision;
    /** 当前实际显示的立绘路径，"" = 不显示。 */
    private String portrait = "";

    public StateEngine(Script script) {
        this.script = script;
        if (script != null && script.nodes() != null) {
            for (Node node : script.nodes()) {
                nodes.put(node.id(), node);
            }
        } else {
            finished = true;
        }
    }

    /** 跳到剧本的起始节点。 */
    public void start() {
        if (script == null) {
            finished = true;
            return;
        }
        moveTo(script.startNodeId());
    }

    /** 每次状态变化 +1，界面靠它判断要不要刷新。 */
    public int revision() {
        return revision;
    }

    public Node current() {
        return current;
    }

    public boolean isFinished() {
        return finished;
    }

    /** 分支节点才有选项。剧情结束后返回 false，否则最后一屏的按钮会一直点得动。 */
    public boolean hasChoices() {
        return !finished && current != null && !current.choices().isEmpty();
    }

    public List<Choice> choices() {
        return hasChoices() ? current.choices() : List.of();
    }

    public String speaker() {
        return current == null ? "" : current.speaker();
    }

    public String text() {
        return current == null ? "" : current.text();
    }

    /**
     * 当前该显示的立绘路径（{@code "命名空间:路径"}），{@code ""} 表示不显示。
     *
     * <p>节点的 {@code portrait} 有三种取值：留空 = 沿用上一个节点的立绘；
     * {@link Node#PORTRAIT_HIDE} = 隐藏；其他 = 换成这张图。
     */
    public String portrait() {
        return portrait;
    }

    /**
     * 线性节点前进。
     *
     * @return {@code false} 表示剧情已经结束（界面该关了），
     * 或者当前是分支节点（必须先调 {@link #select}）
     */
    public boolean advance() {
        if (current == null || finished || !current.choices().isEmpty()) {
            return false;
        }
        moveTo(current.nextId());
        return !finished;
    }

    /**
     * 选中一个分支选项。
     *
     * @return {@code false} 表示 choiceId 不在当前节点的选项里，或者剧情已经结束
     */
    public boolean select(String choiceId) {
        if (current == null || finished || choiceId == null) {
            return false;
        }
        for (Choice choice : current.choices()) {
            if (choice.id().equals(choiceId)) {
                moveTo(choice.nextId());
                return !finished;
            }
        }
        return false;
    }

    /**
     * id 为空或者指向不存在的节点 = 剧情结束。
     * 这里<b>不动 current</b>，让最后一句话留在屏幕上，等玩家再点一下才关界面。
     */
    private void moveTo(String nodeId) {
        Node next = (nodeId == null || nodeId.isEmpty()) ? null : nodes.get(nodeId);
        if (next == null) {
            finished = true;
        } else {
            current = next;
            resolvePortrait(next);
        }
        revision++;
    }

    /**
     * 立绘是"黏"的：留空就沿用上一个节点显示的那张，这样旁白节点不会把立绘弄没，
     * 也不用在每一句上重复写同一个路径。
     */
    private void resolvePortrait(Node node) {
        String requested = node.portrait();
        if (Node.PORTRAIT_HIDE.equals(requested)) {
            portrait = "";
        } else if (!requested.isEmpty()) {
            portrait = requested;
        }
    }
}
