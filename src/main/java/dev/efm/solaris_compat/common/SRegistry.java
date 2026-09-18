package dev.efm.solaris_compat.common;

import dev.efm.solaris_compat.SolarisCompat;
import dev.efm.solaris_compat.common.items.InsolatorSpeedUpgradeItem;
import dev.efm.solaris_compat.common.items.WaterUpgradeItem;
import dev.efm.solaris_compat.common.recipeType.SolarisRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SolarisCompat.MODID);

    public static final CreativeModeTab S_TAB = CreativeModeTab.builder().title(Component.translatable("title.solaris_compat.tab")).build();

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SolarisCompat.MODID);

    public static final RegistryObject<CreativeModeTab> SOLARIS_TAB = TABS.register("solaris", () -> S_TAB);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, SolarisCompat.MODID);

    public static final RegistryObject<RecipeSerializer<?>> SOLARIS_RECIPE_SERIALIZER = RECIPES.register("solaris_shapeless", SolarisRecipe.Serializer::new);

    public static final RegistryObject<Item> WATER_UPGRADE = ITEMS.register("water_upgrade", () -> new WaterUpgradeItem(WaterUpgradeItem.WaterTier.WATER));
    public static final RegistryObject<Item> UPGRADED_WATER_UPGRADE = ITEMS.register("upgraded_water_upgrade", () -> new WaterUpgradeItem(WaterUpgradeItem.WaterTier.UPGRADED_WATER));

    public static final RegistryObject<Item> GOO1_UPGRADE = ITEMS.register("goo1_upgrade", () -> new InsolatorSpeedUpgradeItem(1.2f, 0.10f, 1.2f));
    public static final RegistryObject<Item> GOO2_UPGRADE = ITEMS.register("goo2_upgrade", () -> new InsolatorSpeedUpgradeItem(1.5f, 0.20f, 2f));
    public static final RegistryObject<Item> GOO3_UPGRADE = ITEMS.register("goo3_upgrade", () -> new InsolatorSpeedUpgradeItem(2f, 0.30f, 2f));
    public static final RegistryObject<Item> GOO4_UPGRADE = ITEMS.register("goo4_upgrade", () -> new InsolatorSpeedUpgradeItem(4f, 0.50f, 3f));

    public static void register(IEventBus ibus) {
        ITEMS.register(ibus);
        TABS.register(ibus);
        ibus.addListener(SRegistry::onCreativeTab);
        RECIPES.register(ibus);
    }

    public static void onCreativeTab(BuildCreativeModeTabContentsEvent evt) {
        if (evt.getTab().equals(SOLARIS_TAB.get())) {
            evt.accept(GOO1_UPGRADE.get());
            evt.accept(GOO2_UPGRADE.get());
            evt.accept(GOO3_UPGRADE.get());
            evt.accept(GOO4_UPGRADE.get());
        }
    }
}
