package dev.efm.solaris_resources.common.worldgen;

import java.util.Set;

public class OreGenerationPolicyTest {
    public static void main(String[] args) {
        for (String id : new String[]{"coal_ore", "deepslate_coal_ore", "iron_ore", "deepslate_iron_ore",
                "copper_ore", "deepslate_copper_ore", "gold_ore", "deepslate_gold_ore", "redstone_ore",
                "deepslate_redstone_ore", "lapis_ore", "deepslate_lapis_ore", "diamond_ore",
                "deepslate_diamond_ore", "emerald_ore", "deepslate_emerald_ore", "nether_gold_ore",
                "nether_quartz_ore", "ancient_debris"}) {
            check(OreGenerationPolicy.isOre("minecraft:" + id, false, Set.of()), "vanilla ore: " + id);
        }
        for (String id : new String[]{"dirt", "gravel", "granite", "diorite", "andesite", "tuff",
                "raw_iron_block", "raw_copper_block"}) {
            check(!OreGenerationPolicy.isOre("minecraft:" + id, false, Set.of()), "ordinary block: " + id);
        }
        check(!OreGenerationPolicy.isOre("test:ore_decoration", false, Set.of()), "no name heuristic");
        check(OreGenerationPolicy.isOre("test:tin_ore", true, Set.of()), "tagged mod ore");
        check(!OreGenerationPolicy.isOre("test:tin_ore", false, Set.of()), "removed tag must take effect");
        check(OreGenerationPolicy.isOre("test:tin_ore", false, Set.of("test:tin_ore")), "additional ore");
        check(!OreGenerationPolicy.isOre("test:tin_ore", false, Set.of("test:silver_ore")), "removed extra entry");
        System.out.println("OreGenerationPolicy tests passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
