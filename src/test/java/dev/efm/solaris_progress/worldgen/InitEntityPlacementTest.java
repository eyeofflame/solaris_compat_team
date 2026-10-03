package dev.efm.solaris_progress.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InitEntityPlacementTest {

    @Test
    void hanging_entity_coords_are_moved_to_placement_site() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:item_frame");
        tag.putInt("TileX", 92);
        tag.putInt("TileY", 4);
        tag.putInt("TileZ", 174);
        tag.putUUID("UUID", UUID.randomUUID());

        ResourceLocation id = InitEntityPlacement.prepareForPlacement(
                tag, new Vec3(1.5, 63.5, -2.5), new BlockPos(1, 63, -3));

        assertEquals("minecraft:item_frame", String.valueOf(id));
        assertEquals(1, tag.getInt("TileX"));
        assertEquals(63, tag.getInt("TileY"));
        assertEquals(-3, tag.getInt("TileZ"));
        ListTag pos = tag.getList("Pos", 6);
        assertEquals(3, pos.size());
        assertEquals(1.5, pos.getDouble(0), 1e-9);
        assertEquals(63.5, pos.getDouble(1), 1e-9);
        assertEquals(-2.5, pos.getDouble(2), 1e-9);
        assertFalse(tag.hasUUID("UUID"));
    }

    @Test
    void entity_without_tile_coords_still_gets_new_pos() {
        CompoundTag tag = new CompoundTag();
        tag.putString("id", "minecraft:villager");

        ResourceLocation id = InitEntityPlacement.prepareForPlacement(
                tag, new Vec3(10.0, 64.0, 20.0), new BlockPos(10, 64, 20));

        assertNotNull(id);
        ListTag pos = tag.getList("Pos", 6);
        assertEquals(3, pos.size());
        assertEquals(10.0, pos.getDouble(0), 1e-9);
    }

    @Test
    void empty_or_invalid_id_is_rejected() {
        assertNull(InitEntityPlacement.prepareForPlacement(new CompoundTag(), Vec3.ZERO, BlockPos.ZERO));

        CompoundTag blank = new CompoundTag();
        blank.putString("id", "");
        assertNull(InitEntityPlacement.prepareForPlacement(blank, Vec3.ZERO, BlockPos.ZERO));

        CompoundTag bad = new CompoundTag();
        bad.putString("id", "not a valid id!");
        assertNull(InitEntityPlacement.prepareForPlacement(bad, Vec3.ZERO, BlockPos.ZERO));
    }
}
