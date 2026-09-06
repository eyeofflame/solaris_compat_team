package dev.efm.solaris_compat.rpg_ui.session;

import dev.efm.solaris_compat.rpg_ui.data.DialogueChoice;
import dev.efm.solaris_compat.rpg_ui.data.DialogueNode;
import dev.efm.solaris_compat.rpg_ui.data.DialogueScript;
import net.minecraft.server.level.ServerPlayer;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DialogueSession {
    private static final Map<UUID, DialogueSession> ACTIVE = new HashMap<>();
    private final ServerPlayer player;
    private final DialogueScript script;
    private String currentNodeId;

    private DialogueSession(ServerPlayer player, DialogueScript script) {
        this.player = player;
        this.script = script;
        this.currentNodeId = script.startNodeId();
    }

    public static DialogueSession open(ServerPlayer player, DialogueScript script) {
        DialogueSession session = new DialogueSession(player, script);
        ACTIVE.put(player.getUUID(), session);
        return session;
    }

    @Nullable
    public static DialogueSession of(ServerPlayer player) {
        return ACTIVE.get(player.getUUID());
    }

    public void close() {
        ACTIVE.remove(player.getUUID());
    }

    public DialogueNode current() {
        return script.getNode(currentNodeId);
    }

    public boolean isFinished() {
        DialogueNode node = current();
        return node == null || (!node.hasChoices() && node.nextId() == null);
    }

    @Nullable
    public DialogueNode advance(@Nullable String choiceId) {
        DialogueNode cur = current();
        if (cur == null) return null;
        String next;
        if (cur.hasChoices()) {
            next = cur.choices().stream().map(DialogueChoice::nextId).filter(choiceId::equals).findFirst().orElse(null);
        } else {
            next = cur.nextId();
        }

        if (next == null) {
            close();
            return null;
        }
        currentNodeId = next;
        return current();
    }
}
