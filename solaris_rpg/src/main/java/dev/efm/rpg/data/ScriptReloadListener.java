package dev.efm.rpg.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.lowdragmc.lowdraglib.LDLib;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 从数据包读剧本：{@code data/<命名空间>/solaris_rpg/<名字>.json}，剧本 id 就是
 * {@code <命名空间>:<名字>}（例如 {@code mypack:olivia_intro}）。
 *
 * <p>格式见 {@link ScriptJson}。每次 /reload 会整体替换数据包那一层，内置剧本不受影响。
 * 单个文件解析失败只记日志跳过，不让整个 reload 挂掉。
 */
public class ScriptReloadListener extends SimpleJsonResourceReloadListener {

    public static final String DIRECTORY = "solaris_rpg";

    private static final Gson GSON = new GsonBuilder().create();

    public ScriptReloadListener() {
        super(GSON, DIRECTORY);
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        Map<String, Script> parsed = new LinkedHashMap<>();
        files.forEach((id, json) -> {
            try {
                parsed.put(id.toString(), ScriptJson.parse(id.toString(), json.getAsJsonObject()));
            } catch (Exception e) {
                // 一个剧本写坏了不该拖垮其它剧本，更不该让 /reload 失败
                LDLib.LOGGER.error("剧本解析失败，已跳过: {}", id, e);
            }
        });
        ScriptRegistry.setDatapack(parsed);
        LDLib.LOGGER.info("从数据包加载了 {} 个剧本", parsed.size());
    }
}
