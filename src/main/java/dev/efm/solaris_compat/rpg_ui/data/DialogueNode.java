package dev.efm.solaris_compat.rpg_ui.data;


import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话节点
 */
public record DialogueNode(String id, String speaker, Component text, String nextId, List<DialogueChoice> choices) {
    public boolean hasChoices() {
        return !choices.isEmpty();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("speaker", speaker);
        tag.putString("text", Component.Serializer.toJson(text));
        if (nextId != null) {
            tag.putString("next", nextId);
        }
        if (!choices.isEmpty()) {
            ListTag listTag = new ListTag();
            for (DialogueChoice choice : choices) {
                listTag.add(choice.serializeNBT());
            }
            tag.put("choices", listTag);
        }
        return tag;
    }

    public static DialogueNode deserializeNBT(CompoundTag tag) {
        List<DialogueChoice> choices = new ArrayList<>();
        if (tag.contains("choices", Tag.TAG_LIST)) {
            for (Tag t : tag.getList("choices", Tag.TAG_COMPOUND)) {
                choices.add(DialogueChoice.deserializeNBT((CompoundTag) t));
            }
        }
        String next = tag.contains("next") ? tag.getString("next") : null;
        String speaker = tag.getString("speaker");
        return new DialogueNode(tag.getString("id"), speaker.isEmpty() ? null : speaker, Component.Serializer.fromJson(tag.getString("text")), next, choices);
    }
}
