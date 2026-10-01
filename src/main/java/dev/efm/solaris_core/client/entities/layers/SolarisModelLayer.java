package dev.efm.solaris_core.client.entities.layers;

import dev.efm.solaris_core.SolarisCore;
import dev.efm.solaris_core.helper.GameHelper;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class SolarisModelLayer {
    public static final ModelLayerLocation NUCLEAR_BOMB = new ModelLayerLocation(GameHelper.buildRes(SolarisCore.MODID, "nuclear_bomb"), "main");
}
