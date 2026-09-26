package dev.efm.mekanism_agriculture.client;

import dev.efm.mekanism_agriculture.common.registration.MekContainerTypes;
import dev.efm.mekanism_agriculture.common.registration.TileEntityMekInfusioner;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;

public class ClientSetup {

    public static void clientSetup(FMLClientSetupEvent evt) {
        evt.enqueueWork(() -> MenuScreens.register(MekContainerTypes.MEK_INFUSIONER.get(), GuiMekInfusioner::new));
    }

    /**
     * 不能再用 {@code GuiElectricMachine} —— 它绑死 {@code TileEntityElectricMachine},
     * 且依赖 {@code getEnergyContainer()}/{@code getScaledProgress()}/{@code getWarningCheck(...)}
     * 这些旧基类专有的 API。这里改用 {@link GuiConfigurableTile}(方块实体实现了
     * {@code ISideConfiguration},所以侧向配置与物流配置页签是现成的),
     * 能量条 / 能量页签 / 进度条手动挂上(写法参照 Mekanism 的 GuiModificationStation)。
     *
     * <p>注意:不要用 {@code .warning(...)} + {@code getWarningCheck(RecipeError)},那些是旧基类专有的。
     */
    public static class GuiMekInfusioner extends GuiConfigurableTile<TileEntityMekInfusioner, MekanismTileContainer<TileEntityMekInfusioner>> {

        public GuiMekInfusioner(MekanismTileContainer<TileEntityMekInfusioner> container, Inventory inv, Component title) {
            super(container, inv, title);
            dynamicSlots = true;
        }

        @Override
        protected void addGuiElements() {
            // 自动带上槽位,以及升级/红石/安全页签
            super.addGuiElements();
            addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 164, 15));
            addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), tile::getActive));
            addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.BAR, this, 100, 35));
        }

        @Override
        protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
            renderTitleText(guiGraphics);
            drawString(guiGraphics, playerInventoryTitle, inventoryLabelX, inventoryLabelY, titleTextColor());
            super.drawForegroundText(guiGraphics, mouseX, mouseY);
        }
    }
}
