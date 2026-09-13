package dev.efm.rpg;

import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * 对话事件的挂钩点。
 *
 * <p>存在的意义是让网络包处理器<b>不直接引用 KubeJS 类</b>。本项目里 KubeJS 是可选依赖
 * （build.gradle 里 compileOnly + runtimeOnly，mods.toml 里也没声明），所以公共代码不能碰它
 * ——否则 KubeJS 不在时会 NoClassDefFoundError。
 *
 * <p>做法：KubeJS 插件在 {@code init()} 时往这里注册一个 listener，那个 lambda 所在的类
 * 只会在 KubeJS 存在时被加载。
 */
public final class DialogueHooks {

    /** 玩家在对话里选了某个选项。 */
    @FunctionalInterface
    public interface ChoiceListener {
        void onChoice(ServerPlayer player, String scriptId, String nodeId, String choiceId);
    }

    private static final List<ChoiceListener> CHOICE_LISTENERS = new ArrayList<>();

    private DialogueHooks() {
    }

    public static void addChoiceListener(ChoiceListener listener) {
        if (listener != null) {
            CHOICE_LISTENERS.add(listener);
        }
    }

    public static void fireChoice(ServerPlayer player, String scriptId, String nodeId, String choiceId) {
        for (ChoiceListener listener : CHOICE_LISTENERS) {
            listener.onChoice(player, scriptId, nodeId, choiceId);
        }
    }
}
