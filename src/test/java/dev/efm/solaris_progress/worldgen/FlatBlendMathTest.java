package dev.efm.solaris_progress.worldgen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlatBlendMathTest {
    @Test void smoothstep_clamps_and_is_monotonic() {
        assertEquals(0.0, FlatBlendMath.smoothstep(0.0), 1e-9);
        assertEquals(1.0, FlatBlendMath.smoothstep(1.0), 1e-9);
        assertEquals(0.5, FlatBlendMath.smoothstep(0.5), 1e-9);
        assertTrue(FlatBlendMath.smoothstep(0.2) < FlatBlendMath.smoothstep(0.8));
    }

    @Test void chebyshev_uses_abs_max() {
        assertEquals(5.0, FlatBlendMath.chebyshev(-5.0, 3.0), 1e-9);
        assertEquals(3.0, FlatBlendMath.chebyshev(2.0, -3.0), 1e-9);
    }

    @Test void blendFactor_zero_inside_one_outside() {
        assertEquals(0.0, FlatBlendMath.blendFactor(100.0, 128.0, 32.0), 1e-9);
        assertEquals(1.0, FlatBlendMath.blendFactor(160.0, 128.0, 32.0), 1e-9);
        assertEquals(0.5, FlatBlendMath.blendFactor(144.0, 128.0, 32.0), 1e-9);
    }

    @Test void blendFactor_handles_zero_width() {
        assertEquals(0.0, FlatBlendMath.blendFactor(127.9, 128.0, 0.0), 1e-9);
        assertEquals(1.0, FlatBlendMath.blendFactor(128.0, 128.0, 0.0), 1e-9);
    }

    @Test void outside_matches_blend_width() {
        assertFalse(FlatBlendMath.outside(159.9, 128.0, 32.0));
        assertTrue(FlatBlendMath.outside(160.0, 128.0, 32.0));
    }

    @Test void flatDensity_crosses_at_flatY() {
        assertTrue(FlatBlendMath.flatDensity(64.0, 63.0) > 0);
        assertEquals(0.0, FlatBlendMath.flatDensity(64.0, 65.0), 1e-9);
        assertTrue(FlatBlendMath.flatDensity(64.0, 66.0) < 0);
    }

    @Test void blend_endpoints() {
        assertEquals(7.0, FlatBlendMath.blend(7.0, 99.0, 0.0), 1e-9);
        assertEquals(99.0, FlatBlendMath.blend(7.0, 99.0, 1.0), 1e-9);
    }

    @Test void edgeDither_is_bounded_and_deterministic() {
        for (int x = -300; x <= 300; x += 7) {
            for (int z = -300; z <= 300; z += 11) {
                double d = FlatBlendMath.edgeDither(x, z);
                assertTrue(d >= -2.0 && d <= 2.0);
                assertEquals(d, FlatBlendMath.edgeDither(x, z), 1e-12);
            }
        }
    }

    @Test void resolve_outside_returns_vanilla() {
        assertEquals(42.0, FlatBlendMath.resolve(1000.0, 128.0, 32.0, 64.0, 0.0, 42.0), 1e-9);
    }

    @Test void resolve_inside_returns_flat() {
        // flatDensity(64, 50) = 15，t = 0
        assertEquals(15.0, FlatBlendMath.resolve(0.0, 128.0, 32.0, 64.0, 50.0, 42.0), 1e-9);
    }

    @Test void resolve_midpoint_blends() {
        // d = 144 → t = 0.5；flat = 15，vanilla = 42 → 28.5
        assertEquals(28.5, FlatBlendMath.resolve(144.0, 128.0, 32.0, 64.0, 50.0, 42.0), 1e-9);
    }
}
