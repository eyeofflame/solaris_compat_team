package dev.efm.solaris_compat.kjs;

import dev.efm.rpg.SFactory;
import dev.efm.rpg.SHolder;
import dev.efm.rpg.data.Node;
import dev.efm.rpg.data.Script;
import dev.efm.rpg.data.ScriptRegistry;
import dev.latvian.mods.kubejs.typings.Info;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 绑定成全局 {@code SolarisDialogue} 的门面，和 KubeJS 自己绑定 {@code JsonIO} / {@code Utils}
 * 一个路子——静态方法的类，脚本里直接 {@code SolarisDialogue.xxx(...)}。
 *
 * <p><b>为什么叫 SolarisDialogue 而不是 SolarisRPG：</b>KubeJS 会把每个注册过的
 * {@code EventGroup} 按 {@code group.name} 自动绑成全局（见 {@code BuiltinKubeJSPlugin
 * .registerBindings} 里那个 {@code EventGroup.getGroups()} 循环），而事件组就叫
 * {@code SolarisRPG}。如果这里再绑一个同名的类，就会把事件组覆盖掉，
 * 脚本里 {@code SolarisRPG.scripts(...)} 会直接报 "no public method named scripts"。
 * LDLib 也是这个形状：事件组叫 {@code LDLibUI}，API 类叫 {@code BlockUIFactory}。
 */
public class SolarisDialogueJS {

    /** 脚本里收起立绘用：{@code .portrait(SolarisDialogue.HIDE_PORTRAIT)} */
    public static final String HIDE_PORTRAIT = Node.PORTRAIT_HIDE;

    private SolarisDialogueJS() {
    }

    /**
     * 打开一个剧本给玩家看。只能在服务端调用。
     *
     * @return 打开成功返回 true；剧本不存在或调用方不是玩家返回 false
     */
    @Info("Opens a dialogue script for a player. Server side only.")
    public static boolean open(Player player, String scriptId) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        Script script = ScriptRegistry.get(scriptId);
        if (script == null) {
            return false;
        }
        return SFactory.INSTANCE.openUI(new SHolder(script), serverPlayer);
    }

    /** 某个剧本 id 是否已注册（内置 / 数据包 / KubeJS 任意一层都算）。 */
    @Info("Returns true if a script with this id is registered.")
    public static boolean has(String scriptId) {
        return ScriptRegistry.get(scriptId) != null;
    }
}
