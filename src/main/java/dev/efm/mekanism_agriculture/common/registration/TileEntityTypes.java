package dev.efm.mekanism_agriculture.common.registration;

import dev.efm.mekanism_agriculture.MekanismAgriculture;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.tile.base.TileEntityMekanism;

public class TileEntityTypes {
    public static final TileEntityTypeRegistryObject<TileEntityMekInfusioner> MEK_INFUSIONER = MekanismAgriculture.TILE_ENTITY_TYPES
            .register(MekBlocks.MEK_INFUSIONER, TileEntityMekInfusioner::new, TileEntityMekanism::tickServer, TileEntityMekanism::tickClient);

    public static void init(){}
}
