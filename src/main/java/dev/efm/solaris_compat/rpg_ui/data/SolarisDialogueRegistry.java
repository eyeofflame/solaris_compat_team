package dev.efm.solaris_compat.rpg_ui.data;

import net.minecraft.network.chat.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SolarisDialogueRegistry {
    private static final Map<String, DialogueScript> SCRIPTS = new LinkedHashMap<>();

    public static void register(String id, DialogueScript script) {
        SCRIPTS.put(id, script);
    }

    public static DialogueScript get(String id){
        return SCRIPTS.get(id);
    }

    public static void registerDefaults() {
        Map<String, DialogueNode> nodes = new LinkedHashMap<>();
        nodes.put("start", new DialogueNode("start", "神人", Component.literal("Fuck you"), null, List.of(
                new DialogueChoice("你妈", "fucked"),
                new DialogueChoice("操你妈", "fucking")
        )));

        nodes.put("fucked", new DialogueNode("fucked", "神人", Component.literal("你妈妈"), null, List.of()));
        nodes.put("fucking", new DialogueNode("fucking", "神人", Component.literal("妈妈的"), null, List.of()));
        register("test", new DialogueScript("test", "start", nodes));
    }
}
