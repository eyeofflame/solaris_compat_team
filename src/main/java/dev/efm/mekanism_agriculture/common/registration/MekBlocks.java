package dev.efm.mekanism_agriculture.common.registration;

import dev.efm.mekanism_agriculture.MekanismAgriculture;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.item.block.machine.ItemBlockMachine;
import mekanism.common.registration.impl.BlockRegistryObject;
import net.minecraft.world.level.material.MapColor;

public class MekBlocks {
    public static final BlockRegistryObject<BlockTile<TileEntityMekInfusioner, BlockTypeTile<TileEntityMekInfusioner>>, ItemBlockMachine> MEK_INFUSIONER =
            MekanismAgriculture.BLOCKS.register("agriculture_infusioner", () -> new BlockTile<>(MekBlockTypes.MEK_INFUSIONER, properties -> properties.mapColor(MapColor.METAL)),
                    ItemBlockMachine::new);

    public static void init(){}
}
