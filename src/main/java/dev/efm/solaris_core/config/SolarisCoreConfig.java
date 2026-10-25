package dev.efm.solaris_core.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * solaris_core 的通用配置。
 */
public final class SolarisCoreConfig {

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TRIGGER_ITEMS;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment("多方块机器相关").push("multiblock");
        TRIGGER_ITEMS = builder
                .comment("可以触发多方块成型的物品 ResourceLocation 列表，例如 [\"immersiveengineering:hammer\", \"minecraft:stick\"]")
                .defineList("trigger_items", List.of(), o -> o instanceof String s && ResourceLocation.tryParse(s) != null);
        builder.pop();
        SPEC = builder.build();
    }

    private SolarisCoreConfig() {
    }

    public static boolean isTriggerItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return false;
        }
        for (String candidate : TRIGGER_ITEMS.get()) {
            if (id.equals(ResourceLocation.tryParse(candidate))) {
                return true;
            }
        }
        return false;
    }
}
