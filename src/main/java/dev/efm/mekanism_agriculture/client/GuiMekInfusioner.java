package dev.efm.mekanism_agriculture.client;

import dev.efm.mekanism_agriculture.common.registration.TileEntityMekInfusioner;
import mekanism.client.gui.GuiConfigurableTile;
import mekanism.client.gui.element.bar.GuiVerticalPowerBar;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

/**
 * 注魔机的 GUI。
 *
 * <p>不能用 {@code GuiElectricMachine} —— 它绑死 {@code TileEntityElectricMachine},且依赖
 * {@code getWarningCheck(RecipeError)} 这些旧基类专有的 API。这里改用 {@link GuiConfigurableTile}
 * (方块实体实现了 {@code ISideConfiguration},所以侧向配置与物流配置页签是现成的),
 * 能量条 / 能量页签 / 进度条手动挂上。
 *
 * <p>进度条的位置在这里定,JEI 的配方点击区复用同一组常量,别只改一处。
 */
public class GuiMekInfusioner extends GuiConfigurableTile<TileEntityMekInfusioner, MekanismTileContainer<TileEntityMekInfusioner>> {

    /** 进度条左上角在 GUI 中的坐标;尺寸取自 {@link ProgressType#BAR}。 */
    public static final int PROGRESS_X = 100;
    public static final int PROGRESS_Y = 35;

    public GuiMekInfusioner(MekanismTileContainer<TileEntityMekInfusioner> container, Inventory inv, Component title) {
        super(container, inv, title);
        dynamicSlots = true;
    }

    @Override
    protected void addGuiElements() {
        // 自动带上槽位,以及升级/红石/安全/侧向配置页签
        super.addGuiElements();
        addRenderableWidget(new GuiVerticalPowerBar(this, tile.getEnergyContainer(), 164, 15));
        addRenderableWidget(new GuiEnergyTab(this, tile.getEnergyContainer(), tile::getActive));
        addRenderableWidget(new GuiProgress(tile::getScaledProgress, ProgressType.BAR, this, PROGRESS_X, PROGRESS_Y));
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        renderTitleText(guiGraphics);
        drawString(guiGraphics, playerInventoryTitle, inventoryLabelX, inventoryLabelY, titleTextColor());
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}
