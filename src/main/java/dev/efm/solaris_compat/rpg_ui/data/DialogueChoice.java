package dev.efm.solaris_compat.rpg_ui.data;

import net.minecraft.nbt.CompoundTag;

/**
 * 分支
 *
 * @param text   选项显示的文本
 * @param nextId 选中后跳转的对话id
 */
public record DialogueChoice(String text, String nextId) {
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("text", text);
        tag.putString("next", nextId);
        return tag;
    }

    public static DialogueChoice deserializeNBT(CompoundTag tag) {
        return new DialogueChoice(tag.getString("text"), tag.getString("next"));
    }
}
