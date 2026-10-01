package dev.efm.rpg.network;

import com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;
import dev.efm.rpg.SHolder;
import net.minecraft.server.level.ServerPlayer;

/**
 * 服务端对客户端对话包的公共校验：玩家当前正打开的是不是指定剧本的对话界面。
 *
 * <p>客户端不可信。伪造的对话包会触发整合包脚本里的逻辑（发物品、加任务进度……），
 * 所以凡是"客户端说发生了什么"的包都要先过这一关。
 */
final class RpgValidation {

    private RpgValidation() {
    }

    static boolean isDialogueOpen(ServerPlayer player, String scriptId) {
        if (!(player.containerMenu instanceof ModularUIContainer container)) {
            return false;
        }
        var modularUI = container.getModularUI();
        if (modularUI == null || !(modularUI.holder instanceof SHolder holder)) {
            return false;
        }
        return holder.script().scriptId().equals(scriptId);
    }
}
