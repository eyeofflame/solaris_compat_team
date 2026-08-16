package dev.efm.solaris_compat.common;

import dev.efm.solaris_compat.Solaris_compat;
import dev.efm.solaris_compat.common.items.VillagerContractItem;
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
    public static final DeferredRegister<Item> ITEM_DEFERRED_REGISTER = DeferredRegister.create(ForgeRegistries.ITEMS, Solaris_compat.MODID);
    public static final RegistryObject<Item> EMPTY_CONTRACT = ITEM_DEFERRED_REGISTER.register("empty_contract", VillagerContractItem.EmptyContract::new);
    public static final RegistryObject<Item> VILLAGER_CONTRACT = ITEM_DEFERRED_REGISTER.register("villager_contract", VillagerContractItem.VillagerContract::new);

    public static final CreativeModeTab S_TAB = CreativeModeTab.builder().title(Component.translatable("title.solaris_compat.tab")).icon(() -> EMPTY_CONTRACT.get().getDefaultInstance()).build();

    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Solaris_compat.MODID);

    public static final RegistryObject<CreativeModeTab> SOLARIS_TAB = TABS.register("solaris", () -> S_TAB);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPES = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Solaris_compat.MODID);

    public static final RegistryObject<RecipeSerializer<?>> SOLARIS_RECIPE_SERIALIZER = RECIPES.register("solaris_shapeless", SolarisRecipe.Serializer::new);

    public static void register(IEventBus ibus) {
        ITEM_DEFERRED_REGISTER.register(ibus);
        TABS.register(ibus);
        ibus.addListener(SRegistry::onCreativeTab);
        RECIPES.register(ibus);
    }

    public static void onCreativeTab(BuildCreativeModeTabContentsEvent evt) {
        if (evt.getTab().equals(SOLARIS_TAB.get())) {
            evt.accept(EMPTY_CONTRACT.get());
            evt.accept(VILLAGER_CONTRACT.get());
        }
    }
}
