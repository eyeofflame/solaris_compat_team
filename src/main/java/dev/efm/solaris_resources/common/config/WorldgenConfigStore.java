package dev.efm.solaris_resources.common.config;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class WorldgenConfigStore {
    private final Path file;
    private final Predicate<String> validBlock;
    private final BiConsumer<Path, Exception> logError;
    private volatile Set<String> additionalOreBlocks = Set.of();

    public WorldgenConfigStore(Path file, Predicate<String> validBlock, BiConsumer<Path, Exception> logError) {
        this.file = file;
        this.validBlock = validBlock;
        this.logError = logError;
    }

    public Set<String> additionalOreBlocks() {
        return additionalOreBlocks;
    }

    public boolean reload() {
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(file.getParent());
                JsonObject defaults = new JsonObject();
                defaults.add("additionalOreBlocks", new com.google.gson.JsonArray());
                Files.writeString(file, new GsonBuilder().setPrettyPrinting().create().toJson(defaults)
                        + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
            }
            var json = JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
            var values = json.get("additionalOreBlocks");
            if (values == null || !values.isJsonArray()) {
                throw new IllegalArgumentException("additionalOreBlocks must be an array of block IDs");
            }
            Set<String> parsed = new HashSet<>();
            for (var value : values.getAsJsonArray()) {
                if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
                    throw new IllegalArgumentException("additionalOreBlocks entries must be strings");
                }
                String id = value.getAsString();
                if (!validBlock.test(id)) throw new IllegalArgumentException("Unknown or invalid block ID: " + id);
                parsed.add(id);
            }
            // Publish only after the entire file has been validated; world generation reads immutable snapshots.
            additionalOreBlocks = Set.copyOf(parsed);
            return true;
        } catch (Exception exception) {
            logError.accept(file, exception);
            return false;
        }
    }
}
