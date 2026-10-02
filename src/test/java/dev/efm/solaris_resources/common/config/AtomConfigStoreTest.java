package dev.efm.solaris_resources.common.config;

import com.google.gson.JsonParser;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class AtomConfigStoreTest {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("solaris-atom-config-test");
        var errors = new ArrayList<Path>();
        var store = new AtomConfigStore(directory.resolve("solaris_resources"),
                id -> id.isEmpty() || id.equals("minecraft:raw_iron") || id.equals("minecraft:iron_ingot"),
                (path, error) -> errors.add(path));
        var iron = store.register("iron", "minecraft:raw_iron", 8);
        Path file = directory.resolve("solaris_resources/iron.json");
        check(Files.exists(file), "registration must create the defaults file");
        var json = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        check(json.get("convertedItem").getAsString().equals("minecraft:raw_iron"), "default target");
        check(json.get("ratio").getAsInt() == 8, "default ratio");

        String custom = "{\"convertedItem\":\"minecraft:iron_ingot\",\"ratio\":4}";
        Files.writeString(file, custom);
        var result = store.reload();
        check(result.loaded() == 1 && result.failed() == 0, "reload counts");
        check(iron.settings().convertedItem().equals("minecraft:iron_ingot") && iron.settings().ratio() == 4,
                "existing entry must observe the new settings");
        check(Files.readString(file).equals(custom), "reload must not overwrite custom config");

        var restarted = new AtomConfigStore(file.getParent(), id -> true, (path, error) -> {});
        var restartedIron = restarted.register("iron", "minecraft:raw_iron", 8);
        restarted.reload();
        check(restartedIron.settings().ratio() == 4, "existing config must survive registration");

        for (String invalid : new String[]{"{", "{}", "null",
                "{\"convertedItem\":\"unknown:item\",\"ratio\":8}",
                "{\"convertedItem\":\"minecraft:raw_iron\",\"ratio\":-1}",
                "{\"convertedItem\":\"minecraft:raw_iron\",\"ratio\":1.5}",
                "{\"convertedItem\":\"minecraft:raw_iron\",\"ratio\":\"4\"}",
                "{\"convertedItem\":\"minecraft:raw_iron\",\"ratio\":2147483648}"}) {
            Files.writeString(file, invalid);
            result = store.reload();
            check(result.failed() == 1, "invalid configuration must fail: " + invalid);
            check(iron.settings().ratio() == 4, "failure must retain last valid settings");
            check(Files.readString(file).equals(invalid), "invalid file must be preserved");
        }
        store.register("oxygen", "", 0);
        result = store.reload();
        check(result.loaded() == 1 && result.failed() == 1, "a bad file must not block other entries");
        check(errors.size() == 9, "each failed file must be logged");
        Files.delete(file);
        result = store.reload();
        check(result.failed() == 0 && iron.settings().ratio() == 8, "missing file must regenerate defaults");
        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
        }
        System.out.println("AtomConfigStore tests passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
