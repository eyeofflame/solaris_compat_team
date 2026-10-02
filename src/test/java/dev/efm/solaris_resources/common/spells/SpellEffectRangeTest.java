package dev.efm.solaris_resources.common.spells;

import java.util.List;

public class SpellEffectRangeTest {
    public static void main(String[] args) {
        // Catch off-by-one upper bounds, incorrect vertical anchors and negative-coordinate errors.
        check(SpellEffectRange.WATER.bounds(10, 20, 30).equals(
                new SpellEffectRange.Bounds(9, 19, 29, 12, 22, 32)), "water surrounds hit block");
        check(SpellEffectRange.ABOVE.bounds(10, 20, 30).equals(
                new SpellEffectRange.Bounds(9, 21, 29, 12, 24, 32)), "entity region is above hit block");
        check(SpellEffectRange.WATER.bounds(-10, -20, -30).equals(
                new SpellEffectRange.Bounds(-11, -21, -31, -8, -18, -28)), "negative water coordinates");
        check(SpellEffectRange.ABOVE.bounds(-10, -20, -30).equals(
                new SpellEffectRange.Bounds(-11, -19, -31, -8, -16, -28)), "negative entity coordinates");

        check(SpellEffectRange.previews(List.of()).isEmpty(), "empty spell has no preview");
        check(SpellEffectRange.previews(List.of("ars_nouveau:break", "other:convert_water")).isEmpty(),
                "unrelated glyphs and namespaces have no preview");
        for (String id : List.of("dissolve_water", "assembly_atom", "aggregation_atom")) {
            var previews = SpellEffectRange.previews(List.of("solaris_resources:" + id));
            check(previews.size() == 1 && previews.get(0).range() == SpellEffectRange.ABOVE,
                    "entity effect recognized: " + id);
        }
        var mixed = SpellEffectRange.previews(List.of("ars_nouveau:touch", "solaris_resources:convert_water",
                "solaris_resources:dissolve_water", "solaris_resources:assembly_atom",
                "solaris_resources:aggregation_atom", "solaris_resources:convert_water"));
        check(mixed.size() == 2, "one preview per distinct region");
        check(mixed.get(0).range() == SpellEffectRange.WATER && mixed.get(1).range() == SpellEffectRange.ABOVE,
                "preserve first effect order for each region");
        System.out.println("SpellEffectRange tests passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
