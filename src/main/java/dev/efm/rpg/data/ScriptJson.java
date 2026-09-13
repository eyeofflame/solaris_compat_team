package dev.efm.rpg.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 数据包 JSON → {@link Script}。文件放在 {@code data/<命名空间>/solaris_rpg/<名字>.json}，
 * 剧本 id 就是 {@code <命名空间>:<名字>}。
 *
 * <p>schema 和 {@link ScriptBuilder} 一一对应，缺省值就是 {@link Node} 的默认值：
 * <pre>{@code
 * {
 *   "start": "start",
 *   "nodes": [
 *     { "id": "start", "text": "你推开酒馆的门。", "next": "greet" },
 *     { "id": "greet", "speaker": "奥利维亚", "portrait": "modid:textures/gui/portrait/x.png",
 *       "text": "哟，稀客。", "next": "end" },
 *     { "id": "branch", "speaker": "奥利维亚", "text": "……说吧。",
 *       "choices": [ { "id": "ask", "text": "我想打听点事。", "next": "answer" } ] }
 *   ]
 * }
 * }</pre>
 *
 * {@code portrait} 缺省 = 沿用上一个节点的立绘，{@code "-"} = 收起。
 * {@code next} 缺省 = 剧情到这儿结束。节点同时有 {@code choices} 和 {@code next} 时以 {@code choices} 为准。
 */
public final class ScriptJson {

    private ScriptJson() {
    }

    public static Script parse(String scriptId, JsonObject json) {
        List<Node> nodes = new ArrayList<>();
        for (JsonElement element : asArray(json.get("nodes"))) {
            nodes.add(parseNode(element.getAsJsonObject()));
        }
        return new Script(scriptId, nodes, str(json, "start", ""));
    }

    private static Node parseNode(JsonObject json) {
        List<Choice> choices = new ArrayList<>();
        for (JsonElement element : asArray(json.get("choices"))) {
            choices.add(parseChoice(element.getAsJsonObject()));
        }
        return new Node(
                str(json, "id", ""),
                str(json, "speaker", ""),
                str(json, "text", ""),
                str(json, "portrait", Node.PORTRAIT_KEEP),
                choices,
                str(json, "next", "")
        );
    }

    private static Choice parseChoice(JsonObject json) {
        return new Choice(
                str(json, "id", ""),
                str(json, "text", ""),
                str(json, "next", "")
        );
    }

    private static String str(JsonObject json, String key, String fallback) {
        JsonElement element = json.get(key);
        return element == null || element.isJsonNull() ? fallback : element.getAsString();
    }

    private static JsonArray asArray(JsonElement element) {
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : new JsonArray();
    }
}
