package dev.efm.solaris_compat.kjs;

import dev.efm.rpg.data.Script;
import dev.efm.rpg.data.ScriptBuilder;
import dev.latvian.mods.kubejs.event.EventGroup;
import dev.latvian.mods.kubejs.event.EventHandler;
import dev.latvian.mods.kubejs.event.EventJS;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * KubeJS 事件组。两个都是 <b>server</b> 事件，所以脚本要写在 {@code server_scripts/} 里，
 * 这样 {@code /reload} 能热重载剧情。
 *
 * <pre>{@code
 * SolarisRPG.scripts(event => {
 *   event.create('my_script', script => {
 *     script.start('start')
 *     script.line('start', '你推开酒馆的门。', 'ask')
 *     script.choose('ask', '旁白', '要试试吗？', c => {
 *       c.option('yes', '要', 'done')
 *       c.option('no', '算了', 'done')
 *     })
 *     script.line('done', '结束。', '')
 *   })
 * })
 *
 * SolarisRPG.choice(event => {
 *   if (event.choiceId === 'yes') event.player.give('minecraft:diamond')
 * })
 * }</pre>
 */
public interface SolarisRPGEvents {

    EventGroup GROUP = EventGroup.of("SolarisRPG");

    /** 注册剧本。脚本里用 {@code event.create(id, builder => ...)}。 */
    EventHandler SCRIPTS = GROUP.server("scripts", () -> ScriptsEventJS.class);

    /**
     * 玩家在对话里选了某个选项。
     *
     * <p>这是<b>通知</b>语义，不能否决或改变对话走向——走向在客户端按下按钮那一刻就定了。
     */
    EventHandler CHOICE = GROUP.server("choice", () -> ChoiceEventJS.class);

    class ScriptsEventJS extends EventJS {

        private final Map<String, Script> scripts = new LinkedHashMap<>();

        /**
         * 注册一个剧本。
         *
         * @param id      剧本 id，之后用 {@code SolarisDialogue.open(player, id)} 打开
         * @param builder 在里面用 {@code script.start/line/say/choose} 搭剧情
         */
        public void create(String id, Consumer<ScriptBuilder> builder) {
            if (id == null || id.isEmpty()) {
                return;
            }
            ScriptBuilder script = new ScriptBuilder(id);
            if (builder != null) {
                builder.accept(script);
            }
            scripts.put(id, script.build());
        }

        /** 插件内部用：事件跑完后交给 {@code ScriptRegistry}。 */
        public Map<String, Script> getScripts() {
            return scripts;
        }
    }

    class ChoiceEventJS extends EventJS {

        public final ServerPlayer player;
        public final String scriptId;
        public final String nodeId;
        public final String choiceId;

        public ChoiceEventJS(ServerPlayer player, String scriptId, String nodeId, String choiceId) {
            this.player = player;
            this.scriptId = scriptId;
            this.nodeId = nodeId;
            this.choiceId = choiceId;
        }
    }
}
