package dev.efm.rpg.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * 剧本注册表。目前是内存里的静态表，剧本由 {@link #defaultReg()} 硬编码。
 * 以后要改成从数据包 JSON 加载的话，只要在资源重载时调 {@link #register(Script)} 就行。
 */
public class ScriptRegistry {

    /**
     * 奥利维亚的立绘。这是<b>完整资源路径</b>——Minecraft 的纹理 ResourceLocation 本身就含
     * {@code textures/} 前缀和 {@code .png} 后缀，不用再拼。
     * 文件放在 {@code assets/solaris_compat/textures/gui/portrait/olivia.png}，
     * 资源包对同一路径的覆盖会自动生效。
     */
    private static final String PORTRAIT_OLIVIA = "solaris_compat:textures/gui/portrait/olivia.png";

    private static final HashMap<String, Script> map = new LinkedHashMap<>();

    public static HashMap<String, Script> getMap() {
        return map;
    }

    public static void remove(String scriptId) {
        map.remove(scriptId);
    }

    public static void register(Script script) {
        map.put(script.scriptId(), script);
    }

    /**
     * 注册内置的示例剧本 {@code test}。
     *
     * <p>结构：旁白开场 → 奥利维亚登场（立起立绘）→ 二选一分支 → 两条分支汇合到同一个节点
     * → 旁白结尾（收起立绘）。结尾节点的 {@code nextId} 是 {@code ""}，表示"没有下一节点"，
     * 玩家再点一下界面就关掉。
     */
    public static void defaultReg() {
        List<Node> nodes = new ArrayList<>();

        // 旁白开场。立绘留空 = 沿用上一个节点（此时还没有立绘，所以什么都不显示）
        nodes.add(line("start", "", "你推开酒馆的门，木地板在脚下吱呀作响。", "greet"));

        // 奥利维亚登场，这里才把立绘立起来；后面所有节点都自动沿用
        nodes.add(say("greet", "奥利维亚", PORTRAIT_OLIVIA,
                "哟，稀客。这么晚了还来我这儿——是有事，还是单纯想喝酒？", "branch"));

        // 分支节点：choices 非空时 nextId 不参与
        List<Choice> choices = new ArrayList<>();
        choices.add(new Choice("ask", "我想打听点事。", "answer_ask"));
        choices.add(new Choice("drink", "先来一杯吧。", "answer_drink"));
        nodes.add(new Node("branch", "奥利维亚", "……说吧，我听着。",
                Node.PORTRAIT_KEEP, choices, null));

        nodes.add(line("answer_ask", "奥利维亚", "打听消息可不便宜。不过看在你大半夜跑一趟的份上，这次算你免费。", "join"));
        nodes.add(line("answer_drink", "奥利维亚", "爽快，我就喜欢你这种人。给你倒满，别客气。", "join"));

        // 两条分支在这里汇合
        nodes.add(line("join", "奥利维亚", "不过说真的，最近那些委托有点不对劲，你最好小心点。", "end"));

        // 结尾切回旁白，显式收起立绘；nextId 为 "" = 剧情结束
        nodes.add(new Node("end", "", "她把杯子推回吧台，转身去招呼别的客人。",
                Node.PORTRAIT_HIDE, List.of(), ""));

        register(new Script("test", nodes, "start"));
    }

    /** 对白 / 旁白，立绘沿用上一个节点。 */
    private static Node line(String id, String speaker, String text, String nextId) {
        return new Node(id, speaker, text, Node.PORTRAIT_KEEP, List.of(), nextId);
    }

    /** 带立绘的对白。 */
    private static Node say(String id, String speaker, String portrait, String text, String nextId) {
        return new Node(id, speaker, text, portrait, List.of(), nextId);
    }
}
