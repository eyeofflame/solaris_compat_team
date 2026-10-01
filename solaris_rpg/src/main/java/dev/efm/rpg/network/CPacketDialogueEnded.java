package dev.efm.rpg.network;

import dev.efm.rpg.DialogueEndEvent;
import dev.efm.rpg.DialogueHooks;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：玩家把对话<b>正常走到了结尾</b>。
 *
 * <p>对话推进完全在客户端，服务端原本不知道对话何时结束。这个包补齐这条信息，让服务端能
 * 抛出 {@link DialogueEndEvent} 并触发 KubeJS 的 {@code SolarisRPG.end}。
 *
 * <p>与 {@link CPacketChoiceSelected} 一样要校验（见 {@link RpgValidation}）：确认玩家当前
 * 确实开着这个剧本的对话，防止伪造包触发脚本逻辑。只在"正常走到结尾"时发送，中途 Esc / 断线不发。
 */
public class CPacketDialogueEnded {

    private final String scriptId;
    private final String nodeId;
    private final boolean skipped;

    public CPacketDialogueEnded(String scriptId, String nodeId, boolean skipped) {
        this.scriptId = scriptId == null ? "" : scriptId;
        this.nodeId = nodeId == null ? "" : nodeId;
        this.skipped = skipped;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(scriptId);
        buf.writeUtf(nodeId);
        buf.writeBoolean(skipped);
    }

    public static CPacketDialogueEnded decode(FriendlyByteBuf buf) {
        return new CPacketDialogueEnded(buf.readUtf(), buf.readUtf(), buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && RpgValidation.isDialogueOpen(player, scriptId)) {
                // Forge 事件优先：其它 mod 的服务端逻辑
                MinecraftForge.EVENT_BUS.post(new DialogueEndEvent(player, scriptId, nodeId, skipped));
                // KubeJS 层通过挂钩点解耦，未装 KubeJS 时这里是空转
                DialogueHooks.fireDialogueEnd(player, scriptId, nodeId, skipped);
            }
        });
        context.setPacketHandled(true);
    }
}
