package dev.efm.rpg.data;

import java.util.ArrayList;
import java.util.List;

/**
 * 可变的节点构造器。由 {@link ScriptBuilder} 创建，{@link ScriptBuilder#build()} 时统一转成 {@link Node}。
 *
 * <p>保持可变是必须的——这样 {@code script.say(...).portrait(...)}、
 * {@code script.choose(..., cb).portrait(...)} 才能链式追加。
 */
public class NodeBuilder {

    final String id;
    String speaker = "";
    String text = "";
    String portrait = Node.PORTRAIT_KEEP;
    final List<Choice> choices = new ArrayList<>();
    String nextId = "";

    NodeBuilder(String id) {
        this.id = id == null ? "" : id;
    }

    public NodeBuilder speaker(String speaker) {
        this.speaker = speaker == null ? "" : speaker;
        return this;
    }

    public NodeBuilder text(String text) {
        this.text = text == null ? "" : text;
        return this;
    }

    /**
     * 立绘路径（完整资源路径，形如 {@code solaris_compat:textures/gui/portrait/olivia.png}）。
     *
     * <p>缺省是 {@link Node#PORTRAIT_KEEP}，表示沿用上一个节点显示的那张；
     * 传 {@link Node#PORTRAIT_HIDE} 则收起立绘。
     */
    public NodeBuilder portrait(String portrait) {
        this.portrait = portrait == null ? Node.PORTRAIT_KEEP : portrait;
        return this;
    }

    /** 下一节点 id。空字符串表示剧情到这儿结束。分支节点上这个值不参与。 */
    public NodeBuilder next(String nextId) {
        this.nextId = nextId == null ? "" : nextId;
        return this;
    }

    Node build() {
        return new Node(id, speaker, text, portrait, List.copyOf(choices), nextId);
    }
}
