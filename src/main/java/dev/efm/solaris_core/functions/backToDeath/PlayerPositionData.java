package dev.efm.solaris_core.functions.backToDeath;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class PlayerPositionData {
    public final Vec3 position;
    public final ResourceLocation dimension;

    public PlayerPositionData(Vec3 position, ResourceLocation dimension) {
        this.position = position;
        this.dimension = dimension;
    }
}
