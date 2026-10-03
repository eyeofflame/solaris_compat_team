package dev.efm.solaris_progress;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SpawnFacingTest {

    /** Minecraft 约定：yaw 0 = 南 = +Z（见 Direction.fromYRot 注释）。出生默认朝向必须等于它。 */
    @Test
    void default_spawn_yaw_faces_positive_z() {
        Direction facing = Direction.fromYRot(ForgeEvents.SPAWN_YAW_POSITIVE_Z);
        assertEquals(Direction.SOUTH, facing);
        assertEquals(1, facing.getStepZ());
        assertEquals(0, facing.getStepX());
    }

    /** Olivia：面向 -Z（北）并俯视 30°。 */
    @Test
    void olivia_faces_negative_z_and_looks_down() {
        Vec3 look = Vec3.directionFromRotation(ForgeEvents.OLIVIA_PITCH_DOWN, ForgeEvents.OLIVIA_YAW_FACING_NEGATIVE_Z);
        assertEquals(Direction.NORTH, Direction.fromYRot(ForgeEvents.OLIVIA_YAW_FACING_NEGATIVE_Z));
        // Mth 的三角函数是查表近似，容差放宽到 1e-3
        assertEquals(0.0, look.x, 1e-3);
        assertEquals(-0.5, look.y, 1e-3, "pitch 30 应为向下 30°");
        assertEquals(-Math.cos(Math.toRadians(30.0)), look.z, 1e-3, "应朝向 -Z");
        assertTrue(look.z < 0.0);
    }
}
