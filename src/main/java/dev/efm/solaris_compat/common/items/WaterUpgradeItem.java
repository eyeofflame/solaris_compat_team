package dev.efm.solaris_compat.common.items;

import com.buuz135.functionalstorage.item.UpgradeItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;

public class WaterUpgradeItem extends UpgradeItem {
    private final WaterTier tier;

    public WaterUpgradeItem(WaterTier tier) {
        super(new Properties().stacksTo(16), Type.UTILITY);
        this.tier = tier;
    }

    public WaterTier getTier() {
        return tier;
    }

    public int getGeneration() {
        return switch (tier) {
            case WATER -> 1000;
            case UPGRADED_WATER -> 20000;
        };
    }

    @Override
    public boolean isFoil(ItemStack pStack) {
        return tier == WaterTier.UPGRADED_WATER;
    }

    @Override
    public void addTooltipDetails(@Nullable Key key, ItemStack stack, List<Component> tooltip, boolean advanced) {
        super.addTooltipDetails(key, stack, tooltip, advanced);
        tooltip.add(Component.translatable("tooltip.solaris.water.generation", new DecimalFormat().format(getGeneration())).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.solaris.water.desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.solaris.water.stacking").withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean hasTooltipDetails(@Nullable Key key) {
        return key == null;
    }

    public enum WaterTier {
        WATER,
        UPGRADED_WATER
    }
}
