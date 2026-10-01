package dev.efm.solaris_resources.common.registration;

import dev.efm.solaris_resources.SolarisResources;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class CreativeTabRegistries {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SolarisResources.MODID);

    public static final RegistryObject<CreativeModeTab> SOLARIS_RESOURCES = TABS.register("solaris_resources",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.solaris_resources"))
                    .icon(() -> new ItemStack(ItemRegistries.WATER_CONVERT_TOME.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ItemRegistries.CRYSTALLIZED_WATER.get());
                        output.accept(ItemRegistries.WATER_CONVERT_TOME.get());

                        ItemRegistries.items.forEach(atomItemRegistryObject -> output.accept(atomItemRegistryObject.get()));

                        output.accept(ItemRegistries.WATER_DISSOLVE_TOME.get());
                    })
                    .build());

    public static void register(IEventBus bus) {
        TABS.register(bus);
    }
}
