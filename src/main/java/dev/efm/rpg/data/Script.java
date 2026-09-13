package dev.efm.rpg.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.List;

public record Script(String scriptId, List<Node> nodes, String startNodeId) {
    public CompoundTag serialize() {
        var tag = new CompoundTag();
        tag.putString("scriptId", scriptId);
        tag.putString("startNodeId", startNodeId);

        ListTag listTag = new ListTag();

        nodes.forEach(el -> {
            listTag.add(el.serialize());
        });
        tag.put("nodes", listTag);
        return tag;
    }

    public static Script deserialize(CompoundTag tag) {
        List<Node> nodesArg = new ArrayList<>();
        tag.getList("nodes", Tag.TAG_COMPOUND).forEach(el -> {
            nodesArg.add(Node.deserialize((CompoundTag) el));
        });
        return new Script(
                tag.getString("scriptId"),
                nodesArg,
                tag.getString("startNodeId")
        );
    }
}
