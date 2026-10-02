package dev.efm.solaris_progress.worldgen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InitPlacementMathTest {
    @Test void anchor_centers_odd_size() {
        assertEquals(-119, InitPlacementMath.anchor(0, 239));
        assertEquals(-117, InitPlacementMath.anchor(0, 235));
    }

    @Test void anchor_respects_center() {
        assertEquals(100 - 119, InitPlacementMath.anchor(100, 239));
    }

    @Test void anchor_y_uses_offset() {
        assertEquals(62, InitPlacementMath.anchorY(64.0, -2));
        assertEquals(64, InitPlacementMath.anchorY(64.0, 0));
    }

    @Test void anchor_span_covers_center() {
        int x0 = InitPlacementMath.anchor(0, 239);
        assertTrue(x0 <= 0 && x0 + 239 - 1 >= 0);
    }

    @Test void structure_blocking_covers_core_and_margin() {
        assertTrue(InitPlacementMath.chunkIntersectsProtectedRegion(0, 0, 0, 0, 128, 128));
        assertTrue(InitPlacementMath.chunkIntersectsProtectedRegion(16, 0, 0, 0, 128, 128));
        assertFalse(InitPlacementMath.chunkIntersectsProtectedRegion(17, 0, 0, 0, 128, 128));
        assertTrue(InitPlacementMath.chunkIntersectsProtectedRegion(-16, 0, 0, 0, 128, 128));
    }
}
