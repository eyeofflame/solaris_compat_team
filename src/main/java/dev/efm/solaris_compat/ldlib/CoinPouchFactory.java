package dev.efm.solaris_compat.ldlib;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import dev.efm.solaris_compat.SolarisCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/** 硬币袋界面的 UIFactory：两端同构建树，服务端负责结算。 */
public class CoinPouchFactory extends UIFactory<CoinPouchHolder> {

    public static final CoinPouchFactory INSTANCE = new CoinPouchFactory();

    public CoinPouchFactory() {
        super(ResourceLocation.fromNamespaceAndPath(SolarisCompat.MODID, "coin_pouch_ui"));
    }

    @Override
    protected ModularUI createUITemplate(CoinPouchHolder holder, Player entityPlayer) {
        holder.attachPlayer(entityPlayer);
        return new ModularUI(CoinPouchUI.build(holder, entityPlayer), holder, entityPlayer);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected CoinPouchHolder readHolderFromSyncData(FriendlyByteBuf syncData) {
        return new CoinPouchHolder(syncData.readEnum(CoinPouchHolder.Source.class));
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf syncData, CoinPouchHolder holder) {
        syncData.writeEnum(holder.source());
    }
}
