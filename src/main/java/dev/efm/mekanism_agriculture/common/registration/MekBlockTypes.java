package dev.efm.mekanism_agriculture.common.registration;

import dev.efm.mekanism_agriculture.common.lang.MekLang;
import mekanism.api.Upgrade;
import mekanism.api.math.FloatingLong;
import mekanism.common.block.attribute.*;
import mekanism.common.content.blocktype.BlockTypeTile;

import java.util.EnumSet;

public class MekBlockTypes {
    public static final BlockTypeTile<TileEntityMekInfusioner> MEK_INFUSIONER = createType();

    private static BlockTypeTile<TileEntityMekInfusioner> createType() {
        BlockTypeTile<TileEntityMekInfusioner> type = new BlockTypeTile<>(() -> TileEntityTypes.MEK_INFUSIONER, MekLang.DESC_MEK_INFUSIONER);
        type.add(Attributes.ACTIVE_LIGHT, new AttributeStateFacing(), Attributes.INVENTORY, Attributes.SECURITY, Attributes.REDSTONE, Attributes.COMPARATOR);
        type.add(new AttributeUpgradeSupport(EnumSet.of(Upgrade.SPEED, Upgrade.ENERGY, Upgrade.MUFFLING)));
        type.add(new AttributeGui(() -> MekContainerTypes.MEK_INFUSIONER, null));
        //type.add(AttributeSound);
        type.add(new AttributeEnergy(
                () -> FloatingLong.createConst(500),
                () -> FloatingLong.createConst(200000)
        ));

        return type;
    }

    public static void init(){}
}
