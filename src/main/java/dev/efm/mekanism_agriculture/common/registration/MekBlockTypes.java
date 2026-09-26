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
        // 注意单位:AttributeEnergy 的两个值都是【焦耳(J)】,这是 Mekanism 的内部单位,不是 FE。
        // 换算率由 MekanismConfig.general.forgeConversionRate 决定,默认 2.5(J = FE * 2.5)。
        // 所以想写 500 FE/t / 200 kFE 就得填 1250 / 500000 —— 直接填 500 只会得到 200 FE/t。
        type.add(new AttributeEnergy(
                () -> FloatingLong.createConst(1250),   // 500 FE/t
                () -> FloatingLong.createConst(500000)  // 200 kFE
        ));

        return type;
    }

    public static void init(){}
}
