package dev.efm.rpg.data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 剧本注册表，分三层存：
 * <ul>
 *     <li>{@link #BUILTIN} —— Java 里硬编码的内置剧本，永远保留</li>
 *     <li>{@link #DATAPACK} —— 数据包 {@code data/<ns>/solaris_rpg/*.json}，每次 /reload 整体替换</li>
 *     <li>{@link #SCRIPTED} —— KubeJS 注册的，每次 /reload 整体替换</li>
 * </ul>
 *
 * <p>拆成三层是因为 /reload 时后两者要整体换掉、而内置的不能被冲掉。
 * {@link #getMap()} 返回三者合并后的视图，同 id 时优先级 <b>KubeJS &gt; 数据包 &gt; 内置</b>。
 */
public class ScriptRegistry {

    /**
     * 奥利维亚的立绘。这是<b>完整资源路径</b>——Minecraft 的纹理 ResourceLocation 本身就含
     * {@code textures/} 前缀和 {@code .png} 后缀，不用再拼。
     * 文件放在 {@code assets/solaris_compat/textures/gui/portrait/olivia.png}，
     * 资源包对同一路径的覆盖会自动生效。
     */
    private static final String PORTRAIT_OLIVIA = "solaris_compat:textures/gui/portrait/olivia.png";

    private static final Map<String, Script> BUILTIN = new LinkedHashMap<>();
    private static final Map<String, Script> DATAPACK = new LinkedHashMap<>();
    private static final Map<String, Script> SCRIPTED = new LinkedHashMap<>();
    /** 合并视图，前三者任一变化时由 {@link #rebuild()} 重建。 */
    private static final Map<String, Script> MERGED = new LinkedHashMap<>();

    private ScriptRegistry() {
    }

    /** 合并后的全部剧本。返回的 map 是内部视图，不要改它。 */
    public static Map<String, Script> getMap() {
        return MERGED;
    }

    public static Script get(String scriptId) {
        return scriptId == null ? null : MERGED.get(scriptId);
    }

    /** 注册内置剧本。{@link #defaultReg()} 用这个。 */
    public static void registerBuiltin(Script script) {
        if (script == null) {
            return;
        }
        BUILTIN.put(script.scriptId(), script);
        rebuild();
    }

    /** 整体替换数据包剧本。传进来的 map 会被复制。 */
    public static void setDatapack(Map<String, Script> scripts) {
        DATAPACK.clear();
        if (scripts != null) {
            DATAPACK.putAll(scripts);
        }
        rebuild();
    }

    /** 整体替换 KubeJS 注册的剧本。传进来的 map 会被复制。 */
    public static void setScripted(Map<String, Script> scripts) {
        SCRIPTED.clear();
        if (scripts != null) {
            SCRIPTED.putAll(scripts);
        }
        rebuild();
    }

    /** 清掉数据包和 KubeJS 两层，只留内置剧本。 */
    public static void clearDynamic() {
        DATAPACK.clear();
        SCRIPTED.clear();
        rebuild();
    }

    private static void rebuild() {
        MERGED.clear();
        MERGED.putAll(BUILTIN);
        MERGED.putAll(DATAPACK);
        MERGED.putAll(SCRIPTED);
    }

    /**
     * 注册内置的示例剧本 {@code test}。
     *
     * <p>结构：旁白开场 → 奥利维亚登场（立起立绘）→ 二选一分支 → 两条分支汇合到同一个节点
     * → 旁白结尾（收起立绘）。结尾节点的 {@code nextId} 是 {@code ""}，表示"没有下一节点"，
     * 玩家再点一下界面就关掉。
     */
    public static void defaultReg() {
        // 用链式 builder 写，顺便验证这套 API。
        // 注意 line/say/choose 返回的是 NodeBuilder，链下去只能微调同一个节点，
        // 要开新节点得重新从 script 调——JS 脚本里也是这个写法。
        var script = new ScriptBuilder("test").start("start");

        // 旁白开场。立绘留空 = 沿用上一个节点（此时还没有立绘，所以什么都不显示）
        script.line("start", "你推开酒馆的门，木地板在脚下吱呀作响。", "greet");

        // 奥利维亚登场，这里才把立绘立起来；后面所有节点都自动沿用
        script.say("greet", "奥利维亚",
                        "哟，稀客。这么晚了还来我这儿——是有事，还是单纯想喝酒？", "branch")
                .portrait(PORTRAIT_OLIVIA);

        // 分支节点
        script.choose("branch", "奥利维亚", "……说吧，我听着。", choices -> choices
                .option("ask", "我想打听点事。", "answer_ask")
                .option("drink", "先来一杯吧。", "answer_drink"));

        script.say("answer_ask", "奥利维亚",
                "打听消息可不便宜。不过看在你大半夜跑一趟的份上，这次算你免费。", "join");
        script.say("answer_drink", "奥利维亚",
                "爽快，我就喜欢你这种人。给你倒满，别客气。", "join");

        // 两条分支在这里汇合
        script.say("join", "奥利维亚",
                "不过说真的，最近那些委托有点不对劲，你最好小心点。", "end");

        // 结尾切回旁白，显式收起立绘；nextId 为 "" = 剧情结束
        script.line("end", "她把杯子推回吧台，转身去招呼别的客人。", "")
                .portrait(Node.PORTRAIT_HIDE);

        registerBuiltin(script.build());
    }

    /** 内置剧本的 id 列表，调试用。 */
    public static List<String> builtinIds() {
        return new ArrayList<>(BUILTIN.keySet());
    }
}
