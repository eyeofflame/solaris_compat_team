package dev.efm.solaris_compat.kjs;

import dev.efm.rpg.DialogueHooks;
import dev.efm.rpg.data.ScriptRegistry;
import dev.latvian.mods.kubejs.KubeJSPlugin;
import dev.latvian.mods.kubejs.script.BindingsEvent;
import dev.latvian.mods.kubejs.script.ScriptType;
import dev.latvian.mods.kubejs.util.ClassFilter;

/**
 * KubeJS 插件入口。通过 {@code src/main/resources/kubejs.plugins.txt} 被 KubeJS 发现
 * ——KubeJS 不在时那个文件没人读，这个类也就永远不会被加载，所以不需要额外的存在性判断。
 */
public class SolarisRPGKubeJSPlugin extends KubeJSPlugin {

    @Override
    public void init() {
        // 把"玩家选了选项"接到 KubeJS 事件上。
        // 这个 lambda 是插件类的一部分，只有 KubeJS 存在时才会被加载，
        // 所以公共代码（DialogueHooks / 网络包）不需要引用任何 KubeJS 类型。
        DialogueHooks.addChoiceListener((player, scriptId, nodeId, choiceId) ->
                SolarisRPGEvents.CHOICE.post(
                        new SolarisRPGEvents.ChoiceEventJS(player, scriptId, nodeId, choiceId)));
    }

    @Override
    public void registerEvents() {
        SolarisRPGEvents.GROUP.register();
    }

    @Override
    public void registerClasses(ScriptType type, ClassFilter filter) {
        // 剧情 API 本身用不到这些（builder 是通过回调传进去的），
        // 但放开的话脚本里也能 Java.type 到，方便调试
        filter.allow("dev.efm.rpg");
        filter.allow("dev.efm.solaris_compat.kjs");
    }

    @Override
    public void registerBindings(BindingsEvent event) {
        // 注意这里绑的是 SolarisDialogue，不是 SolarisRPG。
        // SolarisRPG 是事件组名字，KubeJS 会自己把它绑成全局（BuiltinKubeJSPlugin
        // .registerBindings 里遍历 EventGroup.getGroups()），再绑同名的话会把事件组覆盖掉。
        event.add("SolarisDialogue", SolarisDialogueJS.class);
    }

    /**
     * KubeJS 在服务器脚本全部跑完之后（{@code Scripts loaded} 之后）调这个，
     * 所以这里是收集剧本的正确时机。每次 /reload 都会重新收集一遍。
     */
    @Override
    public void onServerReload() {
        var collected = new SolarisRPGEvents.ScriptsEventJS();
        SolarisRPGEvents.SCRIPTS.post(collected);
        // 没有脚本注册时事件对象是空的，于是这一层被清空——正好是 /reload 想要的语义
        ScriptRegistry.setScripted(collected.getScripts());
    }
}
