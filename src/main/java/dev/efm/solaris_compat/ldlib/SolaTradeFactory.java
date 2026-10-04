package dev.efm.solaris_compat.ldlib;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import dev.efm.solaris_compat.SolarisCompat;
import dev.efm.solaris_compat.data.TradeData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SolaTradeFactory extends UIFactory<SolaTradeHolder> {
    public static final SolaTradeFactory INSTANCE = new SolaTradeFactory();

    public SolaTradeFactory() {
        super(ResourceLocation.fromNamespaceAndPath(SolarisCompat.MODID, "trade_ui"));
    }

    @Override
    protected ModularUI createUITemplate(SolaTradeHolder holder, Player entityPlayer) {
        if (holder == null || holder.tradeData() == null) return null;

        return new ModularUI(LDSAPI.createUI(holder, entityPlayer), holder, entityPlayer);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected SolaTradeHolder readHolderFromSyncData(FriendlyByteBuf syncData) {
        return new SolaTradeHolder(TradeData.fromNbt(syncData.readNbt()));
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf syncData, SolaTradeHolder holder) {
        syncData.writeNbt(holder.tradeData().toNbt());
    }
}
