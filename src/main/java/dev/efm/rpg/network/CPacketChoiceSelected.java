package dev.efm.rpg.network;

import com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;
import dev.efm.rpg.DialogueHooks;
import dev.efm.rpg.SHolder;
import dev.efm.rpg.data.Choice;
import dev.efm.rpg.data.Node;
import dev.efm.rpg.data.Script;
import dev.efm.rpg.data.ScriptRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：玩家在对话里选了某个选项。
 *
 * <p><b>服务端不能盲信这个包。</b>不校验的话，客户端可以伪造出任意
 * {@code (scriptId, nodeId, choiceId)} 去触发整合包脚本里的逻辑（发物品、加任务进度……）。
 * 所以 {@link #handle} 里验两件事：
 * <ol>
 *     <li>玩家当前开的确实是以这个剧本为 holder 的对话界面</li>
 *     <li>这个节点里真的有这个选项</li>
 * </ol>
 *
 * <p>这个包只用来"通知"服务端，<b>不参与对话推进</b>——走向在客户端按下按钮的那一刻就定了。
 * 所以网络出问题也不会卡住对话。
 */
public class CPacketChoiceSelected {

    private final String scriptId;
    private final String nodeId;
    private final String choiceId;

    public CPacketChoiceSelected(String scriptId, String nodeId, String choiceId) {
        this.scriptId = scriptId == null ? "" : scriptId;
        this.nodeId = nodeId == null ? "" : nodeId;
        this.choiceId = choiceId == null ? "" : choiceId;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(scriptId);
        buf.writeUtf(nodeId);
        buf.writeUtf(choiceId);
    }

    public static CPacketChoiceSelected decode(FriendlyByteBuf buf) {
        return new CPacketChoiceSelected(buf.readUtf(), buf.readUtf(), buf.readUtf());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && isValid(player)) {
                DialogueHooks.fireChoice(player, scriptId, nodeId, choiceId);
            }
        });
        context.setPacketHandled(true);
    }

    private boolean isValid(ServerPlayer player) {
        // 1. 玩家当前开着的是不是这个剧本的对话界面
        if (!(player.containerMenu instanceof ModularUIContainer container)) {
            return false;
        }
        var modularUI = container.getModularUI();
        if (modularUI == null || !(modularUI.holder instanceof SHolder holder)) {
            return false;
        }
        if (!holder.script().scriptId().equals(scriptId)) {
            return false;
        }

        // 2. 这个节点里真的有这个选项
        Script script = ScriptRegistry.get(scriptId);
        if (script == null) {
            return false;
        }
        for (Node node : script.nodes()) {
            if (!node.id().equals(nodeId)) {
                continue;
            }
            for (Choice choice : node.choices()) {
                if (choice.id().equals(choiceId)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }
}
