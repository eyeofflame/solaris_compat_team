package dev.efm.rpg;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * 对话<b>正常走到结尾</b>时，在服务端抛出的 Forge 事件。
 *
 * <p>在 {@link net.minecraftforge.common.MinecraftForge#EVENT_BUS} 上触发，
 * 其它 mod 用 {@code @SubscribeEvent} 监听即可执行自己的服务端逻辑：
 *
 * <pre>{@code
 * @SubscribeEvent
 * public static void onDialogueEnd(DialogueEndEvent event) {
 *     ServerPlayer player = event.getPlayer();
 *     if ("solaris_rpg:demo".equals(event.getScriptId())) {
 *         player.getInventory().add(new ItemStack(Items.DIAMOND));
 *     }
 * }
 * }</pre>
 *
 * <p><b>语义</b>：只在剧情走完最后一个节点、玩家再点一下关闭界面时触发一次。
 * 玩家中途按 Esc 关闭、断线等<b>不算</b>结束，不会触发。
 *
 * <p><b>不可取消</b>：事件抛出时对话已经结束，没有可回退的走向。
 */
public class DialogueEndEvent extends PlayerEvent {

    private final String scriptId;
    /**
     * 结尾所在的节点 id（对话结束时会停留在最后一个节点上）。可能为空字符串。
     */
    private final String nodeId;
    /**
     * 是否为玩家主动"跳过"。{@code false} = 正常走到结尾，{@code true} = 按 ESC 确认跳过。
     */
    private final boolean skipped;

    public DialogueEndEvent(ServerPlayer player, String scriptId, String nodeId, boolean skipped) {
        super(player);
        this.scriptId = scriptId;
        this.nodeId = nodeId;
        this.skipped = skipped;
    }

    /**
     * 结束对话的玩家。事件只在服务端触发，所以必为 {@link ServerPlayer}。
     */
    public ServerPlayer getPlayer() {
        return (ServerPlayer) getEntity();
    }

    /**
     * 结束的剧本 id，例如 {@code solaris_rpg:demo}。
     */
    public String getScriptId() {
        return scriptId;
    }

    /**
     * 结尾所在的节点 id。
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * 玩家是否主动跳过了对话。{@code false} = 正常走到结尾；{@code true} = 按 ESC 后确认跳过。
     */
    public boolean isSkipped() {
        return skipped;
    }
}
