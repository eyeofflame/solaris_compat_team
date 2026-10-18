package dev.efm.solaris_resources.common.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class MachineConfigStoreTest {

    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("solaris-machine-config-test");
        Path file = directory.resolve("machine/magic_core_I.json");
        var errors = new ArrayList<Path>();
        var store = new MachineConfigStore(file,
                List.of(new MachineConfigStore.OutputSpec("", 1, 2, 0, 2),
                        new MachineConfigStore.OutputSpec("", 1, 5, 2, 6),
                        new MachineConfigStore.OutputSpec("", 1, 9, 50, 60)),
                id -> id.equals("minecraft:iron_ingot") || id.equals("minecraft:gold_ingot"),
                (path, error) -> errors.add(path));

        check(store.reload(), "the first reload must generate the default file");
        check(Files.exists(file), "the missing file must be created");
        check(store.outputs().size() == MachineConfigStore.OUTPUT_SLOTS, "three slots must always exist");
        check(store.outputs().get(0).isDisabled(), "the default first slot has no item configured");
        check(store.outputs().get(2).chanceMin() == 50 && store.outputs().get(2).chanceMax() == 60,
                "the defaults must keep the original 50-60 branch");

        String valid = """
                {
                  "outputs": [
                    {"chanceMin": 0, "chanceMax": 2, "item": "minecraft:iron_ingot", "countMin": 1, "countMax": 2},
                    {"chanceMin": 2, "chanceMax": 6, "item": "minecraft:gold_ingot", "countMin": 1, "countMax": 5}
                  ]
                }
                """;
        Files.writeString(file, valid);
        check(store.reload(), "a valid file must load");
        check(store.outputs().get(0).item().equals("minecraft:iron_ingot"), "the item must be read");
        check(store.outputs().get(0).countMin() == 1 && store.outputs().get(0).countMax() == 2, "the count range must be read");
        check(store.outputs().get(1).chanceMin() == 2 && store.outputs().get(1).chanceMax() == 6, "the chance range must be read");
        check(store.outputs().get(2).isDisabled(), "a missing third slot must be padded as disabled");
        check(store.outputs().size() == MachineConfigStore.OUTPUT_SLOTS, "padding must keep three slots");

        String[] invalid = {
                "{}",
                "{\"outputs\": 3}",
                "{\"outputs\": [3]}",
                "{\"outputs\": [{\"chanceMin\": 0, \"chanceMax\": 2, \"item\": \"unknown:thing\", \"countMin\": 1, \"countMax\": 2}]}",
                "{\"outputs\": [{\"chanceMin\": 2, \"chanceMax\": 2, \"item\": \"minecraft:iron_ingot\", \"countMin\": 1, \"countMax\": 2}]}",
                "{\"outputs\": [{\"chanceMin\": 0, \"chanceMax\": 101, \"item\": \"minecraft:iron_ingot\", \"countMin\": 1, \"countMax\": 2}]}",
                "{\"outputs\": [{\"chanceMin\": 0, \"chanceMax\": 2, \"item\": \"minecraft:iron_ingot\", \"countMin\": 0, \"countMax\": 2}]}",
                "{\"outputs\": [{\"chanceMin\": 0, \"chanceMax\": 2, \"item\": \"minecraft:iron_ingot\", \"countMin\": 1, \"countMax\": 65}]}",
                "{\"outputs\": [{\"chanceMin\": 0, \"chanceMax\": 2, \"item\": \"minecraft:iron_ingot\", \"countMin\": 5, \"countMax\": 2}]}",
                "{\"outputs\": [{\"chanceMin\": 0, \"chanceMax\": 2, \"item\": \"minecraft:iron_ingot\", \"countMin\": 1.5, \"countMax\": 2}]}",
                "{\"outputs\": [{\"chanceMin\": 0, \"chanceMax\": 2, \"item\": 7, \"countMin\": 1, \"countMax\": 2}]}",
                "{\"outputs\": [{}, {}, {}, {}]}"
        };
        for (String broken : invalid) {
            Files.writeString(file, broken);
            check(!store.reload(), "invalid configuration must be rejected: " + broken);
            check(store.outputs().get(0).item().equals("minecraft:iron_ingot"),
                    "a rejected file must keep the last valid settings: " + broken);
            check(Files.readString(file).equals(broken), "the rejected file must be preserved: " + broken);
        }
        check(errors.size() == invalid.length, "every rejected file must be logged, got " + errors.size());

        Files.delete(file);
        check(store.reload(), "a missing file must regenerate the defaults");
        check(store.outputs().get(0).isDisabled(), "regenerated defaults must be disabled again");

        try (var paths = Files.walk(directory)) {
            for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) {
                Files.delete(path);
            }
        }
        System.out.println("MachineConfigStore tests passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
