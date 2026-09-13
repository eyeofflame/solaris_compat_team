package dev.efm.rpg.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

/**
 * 剧情节点。有两种形态：
 * <ul>
 *     <li>{@code choices} 非空 —— 分支节点。走哪条路由玩家决定，此时 {@code nextId} 不参与</li>
 *     <li>{@code choices} 为空 —— 线性节点。直接走 {@code nextId}</li>
 * </ul>
 *
 * <p>{@code speaker}、{@code portrait}、{@code nextId} 都用字符串表示"没有"。
 * 原因：{@code CompoundTag} 存不了 null（{@code StringTag.valueOf(null)} 会抛 NPE），
 * 而 {@code CompoundTag.getString} 读不到 key 时也返回 {@code ""}，所以序列化和反序列化
 * 两端都用 {@code ""} 才对称。
 */
public record Node(String id, String speaker, String text, String portrait, List<Choice> choices, String nextId) {

    /** 立绘留空 = 沿用上一个节点显示的立绘。旁白节点不该把立绘弄没。 */
    public static final String PORTRAIT_KEEP = "";
    /** 立绘设成这个 = 显式隐藏。 */
    public static final String PORTRAIT_HIDE = "-";

    public Node {
        id = id == null ? "" : id;
        speaker = speaker == null ? "" : speaker;
        text = text == null ? "" : text;
        portrait = portrait == null ? PORTRAIT_KEEP : portrait;
        choices = choices == null ? List.of() : choices;
        nextId = nextId == null ? "" : nextId;
    }

    public CompoundTag serialize() {
        var tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("speaker", speaker);
        tag.putString("text", text);
        tag.putString("portrait", portrait);
        if (choices.isEmpty()) {
            tag.putString("nextId", nextId);
        } else {
            ListTag listTag = new ListTag();
            choices.forEach(el -> listTag.add(el.serialize()));
            tag.put("choices", listTag);
        }
        return tag;
    }

    public static Node deserialize(CompoundTag tag) {
        List<Choice> choicesArg = new ArrayList<>();
        tag.getList("choices", Tag.TAG_COMPOUND).forEach(el -> {
            choicesArg.add(Choice.deserialize((CompoundTag) el));
        });
        return new Node(
                tag.getString("id"),
                tag.getString("speaker"),
                tag.getString("text"),
                tag.getString("portrait"),
                choicesArg,
                tag.getString("nextId")
        );
    }
}
