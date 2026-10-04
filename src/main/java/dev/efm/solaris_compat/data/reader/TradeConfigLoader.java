package dev.efm.solaris_compat.data.reader;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.efm.solaris_compat.SolarisCompat;
import dev.efm.solaris_compat.data.TradeData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 扫描 {@code config/solaris_compat/trade/}（即 {@code /sola_export} 的导出目录），
 * 把每个 json 解析成 {@link TradeData} 并按职业缓存。
 *
 * <p>挂在数据包重载链上：开服初次加载和每次 {@code /reload} 都会重扫整个目录，
 * 单文件解析失败只记日志跳过，不影响其它文件。
 * 以文件里的 {@code profession} 字段为准建索引，未注册的职业会跳过。
 */
@Mod.EventBusSubscriber(modid = SolarisCompat.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TradeConfigLoader implements ResourceManagerReloadListener {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 导出与读取共用的目录（相对 config 根）。
     */
    public static final String TRADE_DIR = "solaris_compat/trade";

    private static volatile Map<ResourceLocation, TradeData> cache = Map.of();

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(new TradeConfigLoader());
    }

    /**
     * 按职业取配置；没有对应文件时返回 null。
     */
    public static TradeData get(ResourceLocation profession) {
        return cache.get(profession);
    }

    public static Map<ResourceLocation, TradeData> getAll() {
        return cache;
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager resourceManager) {
        Path dir = FMLPaths.CONFIGDIR.get().resolve(TRADE_DIR);
        Map<ResourceLocation, TradeData> parsed = new HashMap<>();

        // 诊断：确认注册表物品 codec 在当前 ops 下能解出普通物品
        LOGGER.info("诊断: 物品 codec 探测(minecraft:iron) = {}",
                ForgeRegistries.ITEMS.getCodec().parse(JsonOps.INSTANCE, new JsonPrimitive("minecraft:iron")));

        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.walk(dir)) {
                files.filter(Files::isRegularFile)
                        .filter(file -> file.getFileName().toString().endsWith(".json"))
                        .forEach(file -> parseFile(file, parsed));
            } catch (IOException e) {
                LOGGER.error("扫描交易配置目录失败: {}", dir, e);
            }
        } else {
            LOGGER.info("交易配置目录不存在，跳过: {}", dir);
        }

        cache = Map.copyOf(parsed);
        LOGGER.info("已加载 {} 份村民交易配置", cache.size());
    }

    private static void parseFile(Path file, Map<ResourceLocation, TradeData> out) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement json = JsonParser.parseReader(reader);
            TradeData data = TradeData.CODEC.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(error -> LOGGER.error("交易配置解析失败 {}: {}", file, error))
                    .orElse(null);
            if (data == null) {
                return;
            }

            ResourceLocation profession = data.getProfession();
            if (!ForgeRegistries.VILLAGER_PROFESSIONS.containsKey(profession)) {
                LOGGER.warn("交易配置 {} 的职业 {} 未注册，跳过", file, profession);
                return;
            }

            // optionalFieldOf 会把「字段存在但解码失败」静默吞成默认值：
            // 如果原文的 trades 非空但解出来是空的，把被吞掉的错误单独解一遍打出来
            if (json.isJsonObject()) {
                JsonElement rawTrades = json.getAsJsonObject().get("trades");
                if (rawTrades != null && rawTrades.isJsonObject() && rawTrades.getAsJsonObject().size() > 0
                        && data.getTrades().isEmpty()) {
                    Codec.unboundedMap(Codec.STRING, TradeData.Level.CODEC)
                            .parse(JsonOps.INSTANCE, rawTrades)
                            .resultOrPartial(error -> LOGGER.error("trades 字段解码失败（被 optionalFieldOf 吞掉） {}: {}",
                                    file, error));
                }
            }
            LOGGER.info("交易配置 {}: profession={}, 等级数={}", file.getFileName(), profession, data.getTrades().size());

            if (out.put(profession, data) != null) {
                LOGGER.warn("职业 {} 有重复的交易配置，{} 覆盖了前一份", profession, file);
            }
        } catch (IOException | JsonParseException e) {
            LOGGER.error("读取交易配置失败: {}", file, e);
        }
    }
}
