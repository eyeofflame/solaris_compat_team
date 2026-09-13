package dev.efm.rpg.data;

/**
 * 分支选项构造器。在 {@link ScriptBuilder#choose} 的回调里用。
 */
public class ChoiceBuilder {

    private final NodeBuilder parent;

    ChoiceBuilder(NodeBuilder parent) {
        this.parent = parent;
    }

    /** 加一个选项。{@code next} 是选中后要跳到的节点 id。 */
    public ChoiceBuilder option(String id, String text, String next) {
        parent.choices.add(new Choice(id, text, next));
        return this;
    }
}
