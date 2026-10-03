package dev.efm.solaris_progress;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SettlementClaimMathTest {
    @Test void default_region_is_256_chunks() {
        assertEquals(-128, SettlementClaimMath.minBlock(0, 128));
        assertEquals(127, SettlementClaimMath.maxBlock(0, 128));
        assertEquals(-8, SettlementClaimMath.minChunk(0, 128));
        assertEquals(7, SettlementClaimMath.maxChunk(0, 128));
        assertEquals(16, SettlementClaimMath.chunkCount(0, 128));
    }

    @Test void max_block_uses_half_minus_one() {
        assertEquals(-16, SettlementClaimMath.minBlock(0, 16));
        assertEquals(15, SettlementClaimMath.maxBlock(0, 16));
        assertEquals(-1, SettlementClaimMath.minChunk(0, 16));
        assertEquals(0, SettlementClaimMath.maxChunk(0, 16));
        assertEquals(2, SettlementClaimMath.chunkCount(0, 16));
    }

    @Test void odd_center() {
        assertEquals(-11, SettlementClaimMath.minBlock(5, 16));
        assertEquals(20, SettlementClaimMath.maxBlock(5, 16));
        assertEquals(-1, SettlementClaimMath.minChunk(5, 16));
        assertEquals(1, SettlementClaimMath.maxChunk(5, 16));
        assertEquals(3, SettlementClaimMath.chunkCount(5, 16));
    }

    @Test void center_not_aligned() {
        assertEquals(-28, SettlementClaimMath.minBlock(100, 128));
        assertEquals(227, SettlementClaimMath.maxBlock(100, 128));
        assertEquals(-2, SettlementClaimMath.minChunk(100, 128));
        assertEquals(14, SettlementClaimMath.maxChunk(100, 128));
        assertEquals(17, SettlementClaimMath.chunkCount(100, 128));
    }

    @Test void half_not_multiple_of_16() {
        assertEquals(-100, SettlementClaimMath.minBlock(0, 100));
        assertEquals(99, SettlementClaimMath.maxBlock(0, 100));
        assertEquals(-7, SettlementClaimMath.minChunk(0, 100));
        assertEquals(6, SettlementClaimMath.maxChunk(0, 100));
        assertEquals(14, SettlementClaimMath.chunkCount(0, 100));
    }

    @Test void negative_boundary() {
        assertEquals(-1, SettlementClaimMath.minChunk(-1, 1));
        assertEquals(-1, SettlementClaimMath.maxChunk(-1, 1));
        assertEquals(1, SettlementClaimMath.chunkCount(-1, 1));
        assertEquals(-2, SettlementClaimMath.minChunk(-17, 1));
        assertEquals(-2, SettlementClaimMath.maxChunk(-17, 1));
        assertEquals(1, SettlementClaimMath.chunkCount(-17, 1));
    }

    @Test void chunk_range_covers_region() {
        int[][] cases = {{0, 128}, {0, 16}, {5, 16}, {100, 128}, {0, 100}, {-1, 1}, {-17, 1}};
        for (int[] c : cases) {
            int minChunk = SettlementClaimMath.minChunk(c[0], c[1]);
            int maxChunk = SettlementClaimMath.maxChunk(c[0], c[1]);
            assertTrue(minChunk * 16 <= SettlementClaimMath.minBlock(c[0], c[1]),
                    "minChunk covers minBlock for center=" + c[0] + " half=" + c[1]);
            assertTrue(maxChunk * 16 + 15 >= SettlementClaimMath.maxBlock(c[0], c[1]),
                    "maxChunk covers maxBlock for center=" + c[0] + " half=" + c[1]);
        }
    }
}
