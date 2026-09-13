package dev.efm.rpg.data;

import net.minecraft.nbt.CompoundTag;

/**
 * 分支选项。{@code nextId} 是玩家选中后要跳到的节点 id。
 *
 * <p>三个字段都用 {@code ""} 表示"没有"—— {@code CompoundTag} 存不了 null，
 * {@code StringTag.valueOf(null)} 会抛 NPE。见 {@link Node} 的说明。
 */
public record Choice(String id, String text, String nextId) {

    public Choice {
        id = id == null ? "" : id;
        text = text == null ? "" : text;
        nextId = nextId == null ? "" : nextId;
    }

    public CompoundTag serialize() {
        var tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("text", text);
        tag.putString("nextId", nextId);
        return tag;
    }

    public static Choice deserialize(CompoundTag tag) {
        return new Choice(tag.getString("id"), tag.getString("text"), tag.getString("nextId"));
    }
}
