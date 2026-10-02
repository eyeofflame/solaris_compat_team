package dev.efm.solaris_resources.common.worldgen;

import java.util.Set;

public final class OreGenerationPolicy {
    private static final Set<String> VANILLA_ORES = Set.of(
            "minecraft:coal_ore", "minecraft:deepslate_coal_ore",
            "minecraft:iron_ore", "minecraft:deepslate_iron_ore",
            "minecraft:copper_ore", "minecraft:deepslate_copper_ore",
            "minecraft:gold_ore", "minecraft:deepslate_gold_ore",
            "minecraft:redstone_ore", "minecraft:deepslate_redstone_ore",
            "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore",
            "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore",
            "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore",
            "minecraft:nether_gold_ore", "minecraft:nether_quartz_ore", "minecraft:ancient_debris");

    private OreGenerationPolicy() { }

    public static boolean isOre(String blockId, boolean taggedOre, Set<String> additional) {
        return taggedOre || VANILLA_ORES.contains(blockId) || additional.contains(blockId);
    }
}
