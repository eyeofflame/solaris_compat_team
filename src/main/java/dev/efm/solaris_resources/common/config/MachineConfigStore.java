package dev.efm.solaris_resources.common.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * 单台机器的产出配置：三个产出槽各对应一条规则，每条规则给"命中概率区间 + 物品 + 数量区间"。
 * <p>
 * 语义与另外两个配置存储保持一致：缺失文件时生成默认值、已有文件绝不覆盖、
 * 文件整体校验通过后才发布不可变快照；坏文件保留上一份有效值并写日志。
 */
public class MachineConfigStore {

    /** 与机器的输出槽数量一致。 */
    public static final int OUTPUT_SLOTS = 3;
    public static final int MAX_COUNT = 64;

    /**
     * @param item      物品 ID；空字符串表示这个槽没有配置产出
     * @param countMin  最少产出数量（含）
     * @param countMax  最多产出数量（含）
     * @param chanceMin 命中区间下界（含，0~99）
     * @param chanceMax 命中区间上界（不含，1~100）
     */
    public record OutputSpec(String item, int countMin, int countMax, int chanceMin, int chanceMax) {
        public boolean isDisabled() {
            return item.isEmpty();
        }
    }

    /** 未配置的槽位：永不命中。 */
    public static final OutputSpec DISABLED = new OutputSpec("", 1, 1, 0, 1);

    private final Path file;
    private final List<OutputSpec> defaults;
    private final Predicate<String> validItem;
    private final BiConsumer<Path, Exception> logError;

    private volatile List<OutputSpec> outputs;

    public MachineConfigStore(Path file, List<OutputSpec> defaults, Predicate<String> validItem,
                              BiConsumer<Path, Exception> logError) {
        this.file = file;
        this.defaults = padToSlots(defaults);
        this.validItem = validItem;
        this.logError = logError;
        this.outputs = this.defaults;
    }

    /** 当前生效的三条产出规则（不可变快照）。 */
    public List<OutputSpec> outputs() {
        return outputs;
    }

    public boolean reload() {
        try {
            createDefaults();
            JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            JsonElement element = json.get("outputs");
            if (element == null || !element.isJsonArray()) {
                throw new IllegalArgumentException("outputs must be an array");
            }
            List<OutputSpec> parsed = new ArrayList<>(OUTPUT_SLOTS);
            for (JsonElement entry : element.getAsJsonArray()) {
                if (parsed.size() >= OUTPUT_SLOTS) {
                    throw new IllegalArgumentException("at most " + OUTPUT_SLOTS + " outputs are allowed");
                }
                parsed.add(parse(entry));
            }
            outputs = padToSlots(parsed);
            return true;
        } catch (Exception exception) {
            logError.accept(file, exception);
            return false;
        }
    }

    private OutputSpec parse(JsonElement element) {
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("each output must be an object");
        }
        JsonObject object = element.getAsJsonObject();
        String item = string(object, "item");
        if (!item.isEmpty() && !validItem.test(item)) {
            throw new IllegalArgumentException("unknown item: " + item);
        }
        int chanceMin = integer(object, "chanceMin");
        int chanceMax = integer(object, "chanceMax");
        int countMin = integer(object, "countMin");
        int countMax = integer(object, "countMax");
        if (chanceMin < 0 || chanceMax > 100 || chanceMin >= chanceMax) {
            throw new IllegalArgumentException("chance range must satisfy 0 <= chanceMin < chanceMax <= 100");
        }
        if (countMin < 1 || countMax > MAX_COUNT || countMin > countMax) {
            throw new IllegalArgumentException("count range must satisfy 1 <= countMin <= countMax <= " + MAX_COUNT);
        }
        return new OutputSpec(item, countMin, countMax, chanceMin, chanceMax);
    }

    private static String string(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException(key + " must be a string");
        }
        return element.getAsString();
    }

    private static int integer(JsonObject object, String key) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException(key + " must be an integer");
        }
        return element.getAsBigDecimal().intValueExact();
    }

    private static List<OutputSpec> padToSlots(List<OutputSpec> specs) {
        List<OutputSpec> padded = new ArrayList<>(OUTPUT_SLOTS);
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            padded.add(slot < specs.size() ? specs.get(slot) : DISABLED);
        }
        return List.copyOf(padded);
    }

    private void createDefaults() throws java.io.IOException {
        if (Files.exists(file)) {
            return;
        }
        Files.createDirectories(file.getParent());
        JsonObject root = new JsonObject();
        var array = new com.google.gson.JsonArray();
        for (OutputSpec spec : defaults) {
            array.add(toJson(spec));
        }
        root.add("outputs", array);
        Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(root)
                + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
    }

    private static JsonObject toJson(OutputSpec spec) {
        JsonObject object = new JsonObject();
        object.addProperty("chanceMin", spec.chanceMin());
        object.addProperty("chanceMax", spec.chanceMax());
        object.addProperty("item", spec.item());
        object.addProperty("countMin", spec.countMin());
        object.addProperty("countMax", spec.countMax());
        return object;
    }
}
