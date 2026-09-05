package dev.efm.solaris_compat.rpg_ui;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.animation.Transform;
import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.layout.Align;
import com.lowdragmc.lowdraglib.utils.interpolate.Eases;
import com.mojang.blaze3d.platform.Window;
import dev.efm.solaris_compat.rpg_ui.resources.SolaBorderTexture;
import dev.efm.solaris_compat.rpg_ui.widgets.SolarisMainGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class SolarisUIFactory extends UIFactory<SolarisUIFactory.Holder> {
    public static final SolarisUIFactory INSTANCE = new SolarisUIFactory();

    private SolarisUIFactory() {
        super(LDLib.location("solaris_gui"));
    }

    @Override
    protected ModularUI createUITemplate(SolarisUIFactory.Holder holder, Player player) {
        return new ModularUI(holder, player).widget(createMainGroup());
    }

    private SolarisMainGroup createMainGroup() {
        SolarisMainGroup mainGroup = new SolarisMainGroup(1f, 0.25f, 0, 0, Align.BOTTOM_LEFT);
        mainGroup.setBackground(SolaBorderTexture.SOLA_BORDER_BACKGROUND);
        mainGroup.setText(Component.literal("test----------------------------------------------------------------------------------------------"));

        if (LDLib.isClient()) {
            Window win = Minecraft.getInstance().getWindow();
            mainGroup.animation(
                    new Transform()
                            .offset(0, win.getGuiScaledHeight() / 4)
                            .setIn()
                            .duration(600)
                            .ease(Eases.EaseQuadOut)
            );
        }
        return mainGroup;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected Holder readHolderFromSyncData(FriendlyByteBuf friendlyByteBuf) {
        return new Holder();
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf friendlyByteBuf, Holder holder) {

    }

    public static class Holder implements IUIHolder {
        @Override
        public ModularUI createUI(Player player) {
            return null;
        }

        @Override
        public boolean isInvalid() {
            return false;
        }

        @Override
        public boolean isRemote() {
            return LDLib.isRemote();
        }

        @Override
        public void markAsDirty() {
        }
    }
}
