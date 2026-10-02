package dev.efm.solaris_resources.common.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class AtomConfigStore {
    public record Settings(String convertedItem, int ratio) {}
    public record ReloadResult(int loaded, int failed) {}

    public static class Entry {
        private final Settings defaults;
        private volatile Settings settings;

        private Entry(Settings defaults) {
            this.defaults = defaults;
            this.settings = defaults;
        }

        public Settings settings() {
            return settings;
        }
    }

    private final Path directory;
    private final Predicate<String> validItem;
    private final BiConsumer<Path, Exception> logError;
    private final Map<String, Entry> entries = new LinkedHashMap<>();

    public AtomConfigStore(Path directory, Predicate<String> validItem, BiConsumer<Path, Exception> logError) {
        this.directory = directory;
        this.validItem = validItem;
        this.logError = logError;
    }

    public Entry register(String path, String convertedItem, int ratio) {
        if (!path.matches("[a-z0-9_-]+")) throw new IllegalArgumentException("Invalid atom path: " + path);
        Entry entry = new Entry(new Settings(convertedItem, ratio));
        if (entries.putIfAbsent(path, entry) != null) throw new IllegalArgumentException("Duplicate atom: " + path);
        try {
            createDefaults(directory.resolve(path + ".json"), entry.defaults);
        } catch (IOException exception) {
            logError.accept(directory.resolve(path + ".json"), exception);
        }
        return entry;
    }

    public ReloadResult reload() {
        int loaded = 0, failed = 0;
        for (var registered : entries.entrySet()) {
            Path file = directory.resolve(registered.getKey() + ".json");
            Entry entry = registered.getValue();
            try {
                createDefaults(file, entry.defaults);
                JsonObject json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
                var target = json.get("convertedItem");
                var ratio = json.get("ratio");
                if (target == null || !target.isJsonPrimitive() || !target.getAsJsonPrimitive().isString()
                        || ratio == null || !ratio.isJsonPrimitive() || !ratio.getAsJsonPrimitive().isNumber()) {
                    throw new IllegalArgumentException("convertedItem must be a string and ratio must be an integer");
                }
                String item = target.getAsString();
                int count = ratio.getAsBigDecimal().intValueExact();
                if (count < 0 || !validItem.test(item)) {
                    throw new IllegalArgumentException("Invalid convertedItem or negative ratio");
                }
                entry.settings = new Settings(item, count);
                loaded++;
            } catch (Exception exception) {
                logError.accept(file, exception);
                failed++;
            }
        }
        return new ReloadResult(loaded, failed);
    }

    private void createDefaults(Path file, Settings defaults) throws IOException {
        if (Files.exists(file)) return;
        Files.createDirectories(directory);
        String json = new GsonBuilder().setPrettyPrinting().create().toJson(defaults) + System.lineSeparator();
        Files.writeString(file, json, StandardCharsets.UTF_8, java.nio.file.StandardOpenOption.CREATE_NEW);
    }
}
