package dev.efm.solaris_compat.api;

import com.buuz135.functionalstorage.block.tile.FluidDrawerTile;
import com.buuz135.functionalstorage.fluid.BigFluidHandler;
import com.hrznstudio.titanium.component.inventory.InventoryComponent;
import dev.efm.solaris_compat.common.items.WaterUpgradeItem;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class WaterGenerator {
    private WaterGenerator() {
    }

    public static void tick(FluidDrawerTile drawer) {
        InventoryComponent<?> upgrades = drawer.getUtilityUpgrades();
        if (upgrades == null) return;
        int gen = 0;
        for (int i = 0; i < upgrades.getSlots(); i++) {
            if (upgrades.getStackInSlot(i).getItem() instanceof WaterUpgradeItem upgradeItem) {
                gen += upgradeItem.getGeneration();
            }
        }

        if (gen <= 0) return;

        if (fillWater(drawer, gen) > 0) {
            drawer.getFluidHandler().onChange();
        }
    }

    private static int fillWater(FluidDrawerTile drawer, int amount) {
        BigFluidHandler handler = drawer.getFluidHandler();
        FluidStack water = new FluidStack(Fluids.WATER, amount);
        int remaining = amount;
        int gen = 0;
        for (BigFluidHandler.CustomFluidTank tank : handler.getTankList()) {
            if (remaining <= 0) break;
            if (!tank.isFluidValid(water)) continue;
            if (!tank.getFluid().isEmpty() && !tank.getFluid().isFluidEqual(water)) continue;
            int filled = tank.fill(new FluidStack(Fluids.WATER, remaining), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                remaining -= filled;
                gen += filled;
            }
        }
        return gen;
    }
}
