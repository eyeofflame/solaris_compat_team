package dev.efm.rpg.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * 剧本构造器（链式），给 KubeJS 脚本作者用的主 API。
 *
 * <pre>{@code
 * Script script = new ScriptBuilder("demo")
 *         .start("start")
 *         .line("start", "你推开酒馆的门。", "greet")
 *         .say("greet", "奥利维亚", "哟，稀客。", "branch").portrait(PORTRAIT_OLIVIA)
 *         .choose("branch", "奥利维亚", "……说吧。", c -> c
 *                 .option("ask", "我想打听点事。", "answer")
 *                 .option("drink", "先来一杯吧。", "answer"))
 *         .say("answer", "奥利维亚", "知道了。", "")
 *         .build();
 * }</pre>
 *
 * <p>{@code line}/{@code say}/{@code choose} 返回 {@link NodeBuilder}，可以继续链式微调这个节点
 * （最常见的是 {@code .portrait(...)}）。
 */
public class ScriptBuilder {

    private final String scriptId;
    private final List<NodeBuilder> nodes = new ArrayList<>();
    private String startNodeId = "";

    public ScriptBuilder(String scriptId) {
        this.scriptId = scriptId == null ? "" : scriptId;
    }

    /** 起始节点 id。不设的话取第一个加的节点。 */
    public ScriptBuilder start(String nodeId) {
        this.startNodeId = nodeId == null ? "" : nodeId;
        return this;
    }

    /** 旁白线性节点。 */
    public NodeBuilder line(String id, String text, String next) {
        return say(id, "", text, next);
    }

    /** 带说话人的线性节点。 */
    public NodeBuilder say(String id, String speaker, String text, String next) {
        NodeBuilder node = new NodeBuilder(id).speaker(speaker).text(text).next(next);
        nodes.add(node);
        return node;
    }

    /**
     * 分支节点。{@code next} 不参与——走向由玩家选的 {@link Choice} 决定。
     *
     * @param choices 在里面调 {@link ChoiceBuilder#option}
     */
    public NodeBuilder choose(String id, String speaker, String text, Consumer<ChoiceBuilder> choices) {
        NodeBuilder node = new NodeBuilder(id).speaker(speaker).text(text);
        if (choices != null) {
            choices.accept(new ChoiceBuilder(node));
        }
        nodes.add(node);
        return node;
    }

    public Script build() {
        List<Node> built = new ArrayList<>(nodes.size());
        for (NodeBuilder node : nodes) {
            built.add(node.build());
        }
        String start = startNodeId.isEmpty() && !built.isEmpty() ? built.get(0).id() : startNodeId;
        return new Script(scriptId, built, start);
    }
}
