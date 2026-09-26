package dev.efm.mekanism_agriculture.common.registration;

import dev.efm.mekanism_agriculture.MekanismAgriculture;
import mekanism.common.content.blocktype.BlockTypeTile;
import mekanism.common.item.block.machine.ItemBlockMachine;
import mekanism.common.registration.impl.BlockRegistryObject;
import net.minecraft.world.level.material.MapColor;

public class MekBlocks {

    // 两个刻意的设置,原因都写在这里免得以后被"顺手删掉":
    //
    // 1) noOcclusion():模型是个高 30 格(≈1.9 个方块)的细柱,中段远窄于一个完整方块。
    //    若沿用原版默认的整块遮挡,相邻方块朝着它的那一面会被剔除,看过去就是一个个虚空洞
    //    ——之前"下方方块显示为虚空"就是这个根因。
    // 2) 方块类覆写了形状为 1×2×1,让整台机器实心可碰撞(详见 BlockMekInfusioner)。
    //    因为已经关掉遮挡,形状取两格高也不会误触发相邻面剔除。
    public static final BlockRegistryObject<BlockMekInfusioner, ItemBlockMachine> MEK_INFUSIONER =
            MekanismAgriculture.BLOCKS.register("agriculture_infusioner",
                    () -> new BlockMekInfusioner(MekBlockTypes.MEK_INFUSIONER,
                            properties -> properties.mapColor(MapColor.METAL).noOcclusion()),
                    ItemBlockMachine::new);

    public static void init() {
    }
}
