package dev.efm.solaris_resources.common.config;

import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public class WorldgenConfigStoreTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("solaris-worldgen-config");
        Path file = directory.resolve("config/solaris_resources/worldgen.json");
        var errors = new ArrayList<Exception>();
        var store = new WorldgenConfigStore(file, id -> Set.of("test:tin_ore", "test:silver_ore").contains(id),
                (path, error) -> errors.add(error));
        check(store.additionalOreBlocks().isEmpty(), "initial defaults");
        check(store.reload(), "missing file should generate defaults");
        check(JsonParser.parseString(Files.readString(file)).getAsJsonObject()
                .getAsJsonArray("additionalOreBlocks").isEmpty(), "empty defaults in JSON");
        String custom = "{\"additionalOreBlocks\":[\"test:tin_ore\",\"test:tin_ore\"]}";
        Files.writeString(file, custom);
        check(store.reload(), "valid config should load");
        check(store.additionalOreBlocks().equals(Set.of("test:tin_ore")), "deduplicate");
        check(Files.readString(file).equals(custom), "custom file must not be overwritten");
        Set<String> oldSnapshot = store.additionalOreBlocks();
        try {
            oldSnapshot.add("test:silver_ore");
            throw new AssertionError("snapshot must be immutable");
        } catch (UnsupportedOperationException expected) { }
        String[] invalid = {"null", "{}", "{", "{\"additionalOreBlocks\":null}",
                "{\"additionalOreBlocks\":\"test:tin_ore\"}", "{\"additionalOreBlocks\":[1]}",
                "{\"additionalOreBlocks\":[null]}", "{\"additionalOreBlocks\":[\"unknown:ore\"]}",
                "{\"additionalOreBlocks\":[\"test:silver_ore\",\"Invalid ID\"]}"};
        for (String json : invalid) {
            Files.writeString(file, json);
            check(!store.reload(), "invalid config must be rejected: " + json);
            check(store.additionalOreBlocks().equals(Set.of("test:tin_ore")), "retain whole old set");
            check(Files.readString(file).equals(json), "preserve invalid file");
        }
        check(errors.size() == invalid.length, "log each invalid config");
        AtomicReference<Throwable> readerFailure = new AtomicReference<>();
        Thread reader = new Thread(() -> {
            try {
                for (int i = 0; i < 100_000; i++) {
                    Set<String> snapshot = store.additionalOreBlocks();
                    check(snapshot.equals(Set.of("test:tin_ore")) || snapshot.equals(Set.of("test:silver_ore")),
                            "reader must see a complete set");
                }
            } catch (Throwable failure) { readerFailure.set(failure); }
        });
        reader.start();
        for (int i = 0; i < 50; i++) {
            Files.writeString(file, "{\"additionalOreBlocks\":[\"test:" + (i % 2 == 0 ? "silver" : "tin") + "_ore\"]}");
            check(store.reload(), "concurrent reload");
        }
        reader.join();
        check(readerFailure.get() == null, "concurrent read: " + readerFailure.get());
        check(oldSnapshot.equals(Set.of("test:tin_ore")), "old snapshot must not change");
        Files.delete(file);
        check(store.reload() && store.additionalOreBlocks().isEmpty(), "missing file resets to defaults");
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
        }
        System.out.println("WorldgenConfigStore tests passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
