package dev.efm.rpg;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.layout.Align;
import dev.efm.solaris_compat.SolarisCompat;
import dev.efm.solaris_compat.api.SHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SFactory extends UIFactory<SHolder> {
    public static final SFactory INSTANCE = new SFactory();

    public SFactory() {
        super(SHelper.buildRes(SolarisCompat.MODID, "gui"));
    }

    @Override
    protected ModularUI createUITemplate(SHolder holder, Player entityPlayer) {
        if (holder == null) return null;
        var root = new FullScreenGroup();
        root.setAlign(Align.TOP_LEFT);
        root.setBackground(ResourceBorderTexture.EMPTY);

        var main = new WidgetGroup(0, 0, 0, 0).setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        main.setAlign(Align.BOTTOM_CENTER);
        root.addWidget(main);
        root.onLayout(r -> {
            main.setSize(r.getSizeHeight() * (16 / 9), r.getSizeHeight() / 4);
            main.setSelfPosition(r.getSizeWidth() / 2, r.getSizeHeight());
        });

        return new ModularUI(holder, entityPlayer).widget(root);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected SHolder readHolderFromSyncData(FriendlyByteBuf syncData) {
        return new SHolder();
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf syncData, SHolder holder) {

    }
}
