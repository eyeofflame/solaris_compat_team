package dev.efm.solaris_resources.common.registration;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.item.AtomItem;
import dev.efm.solaris_resources.common.item.PresetCasterTome;
import dev.efm.solaris_resources.common.spells.EffectConvertWater;
import dev.efm.solaris_resources.common.spells.EffectDissolveWater;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class ItemRegistries {
    public static List<RegistryObject<AtomItem>> items = new ArrayList<RegistryObject<AtomItem>>();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SolarisResources.MODID);
    public static final RegistryObject<Item> CRYSTALLIZED_WATER = ITEMS.register("crystallized_water", () -> new Item(new Item.Properties().stacksTo(64).setNoRepair()));

    public static void atomRegister(String path, String convertedItem) {
        var item = ITEMS.register("atom_" + path, () -> new AtomItem(convertedItem, false, path));
        var item0 = ITEMS.register("unstable_magic_atom_" + path, () -> new AtomItem(convertedItem, true, path));

        items.add(item);
        items.add(item0);
    }

    public static final RegistryObject<PresetCasterTome> WATER_CONVERT_TOME = ITEMS.register("water_convert_tome",
            () -> new PresetCasterTome(() -> new Spell()
                    .add(MethodTouch.INSTANCE)
                    .add(EffectConvertWater.INSTANCE)
            )
    );

    public static final RegistryObject<PresetCasterTome> WATER_DISSOLVE_TOME = ITEMS.register("water_dissolve_tome",
            () -> new PresetCasterTome(() -> new Spell().add(MethodTouch.INSTANCE).add(EffectDissolveWater.INSTANCE))
    );

    public static void register(IEventBus ibus) {
        ITEMS.register(ibus);

        atomRegister("hydrogen", "");
        atomRegister("oxygen", "");
    }
}
