package dev.efm.solaris_compat.rpg_ui.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.LinkedHashMap;
import java.util.Map;

public record DialogueScript(String id, String startNodeId, Map<String, DialogueNode> nodes) {
    public DialogueNode startNode() {
        return nodes.get(startNodeId);
    }

    public DialogueNode getNode(String nodeId) {
        return nodes.get(nodeId);
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", id);
        tag.putString("start", startNodeId);
        ListTag tags = new ListTag();
        for (DialogueNode node : nodes.values()) {
            tags.add(node.serializeNBT());
        }
        tag.put("nodes", tags);
        return tag;
    }

    public static DialogueScript deserializeNBT(CompoundTag tag) {
        Map<String, DialogueNode> map = new LinkedHashMap<>();
        for (Tag t : tag.getList("nodes", Tag.TAG_COMPOUND)) {
            DialogueNode node = DialogueNode.deserializeNBT((CompoundTag) t);
            map.put(node.id(), node);
        }
        return new DialogueScript(tag.getString("id"), tag.getString("start"), map);
    }
}
