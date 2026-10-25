package dev.efm.solaris_functions.client.entities.layers;

import dev.efm.solaris_functions.SolarisFunctions;
import dev.efm.solaris_core.helper.GameHelper;
import net.minecraft.client.model.geom.ModelLayerLocation;

public class SolarisModelLayer {
    public static final ModelLayerLocation NUCLEAR_BOMB = new ModelLayerLocation(GameHelper.buildRes(SolarisFunctions.MODID, "nuclear_bomb"), "main");
}
