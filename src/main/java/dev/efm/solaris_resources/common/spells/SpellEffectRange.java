package dev.efm.solaris_resources.common.spells;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/** Shared world-space boundaries for spell execution and client previews. Upper bounds are exclusive. */
public enum SpellEffectRange {
    WATER(-1),
    ABOVE(1);

    private final int minYOffset;

    SpellEffectRange(int minYOffset) {
        this.minYOffset = minYOffset;
    }

    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) { }
    public record Preview(SpellEffectRange range, float red, float green, float blue) { }

    public Bounds bounds(int x, int y, int z) {
        return new Bounds(x - 1, y + minYOffset, z - 1, x + 2, y + minYOffset + 3, z + 2);
    }

    public AABB area(BlockPos origin) {
        Bounds bounds = bounds(origin.getX(), origin.getY(), origin.getZ());
        return new AABB(bounds.minX(), bounds.minY(), bounds.minZ(), bounds.maxX(), bounds.maxY(), bounds.maxZ());
    }

    /** Keep the first effect's color when multiple effects share the same region. */
    public static List<Preview> previews(Iterable<String> glyphIds) {
        List<Preview> result = new ArrayList<>();
        EnumSet<SpellEffectRange> seen = EnumSet.noneOf(SpellEffectRange.class);
        for (String id : glyphIds) {
            Preview preview = switch (id) {
                case "solaris_resources:convert_water" -> new Preview(WATER, 0.2F, 0.75F, 1.0F);
                case "solaris_resources:dissolve_water" -> new Preview(ABOVE, 0.3F, 1.0F, 0.65F);
                case "solaris_resources:assembly_atom" -> new Preview(ABOVE, 1.0F, 0.65F, 0.2F);
                case "solaris_resources:aggregation_atom" -> new Preview(ABOVE, 0.8F, 0.4F, 1.0F);
                default -> null;
            };
            if (preview != null && seen.add(preview.range())) result.add(preview);
        }
        return result;
    }
}
