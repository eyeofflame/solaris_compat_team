package dev.efm.mekanism_agriculture.common.registration;

import dev.efm.mekanism_agriculture.MekanismAgriculture;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;

public class MekContainerTypes {
    public static final ContainerTypeRegistryObject<MekanismTileContainer<TileEntityMekInfusioner>> MEK_INFUSIONER =
            MekanismAgriculture.CONTAINER_TYPES.register(MekBlocks.MEK_INFUSIONER, TileEntityMekInfusioner.class);

    public static void init(){}
}
