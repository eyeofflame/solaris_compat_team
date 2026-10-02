package dev.efm.solaris_resources.common.registration;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.item.AtomItem;
import dev.efm.solaris_resources.common.item.PresetCasterTome;
import dev.efm.solaris_resources.common.spells.EffectAggregationAtom;
import dev.efm.solaris_resources.common.spells.EffectAssemblyAtom;
import dev.efm.solaris_resources.common.spells.EffectConvertWater;
import dev.efm.solaris_resources.common.spells.EffectDissolveWater;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ItemRegistries {
    public static List<RegistryObject<AtomItem>> items = new ArrayList<RegistryObject<AtomItem>>();
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SolarisResources.MODID);
    public static final RegistryObject<Item> CRYSTALLIZED_WATER = ITEMS.register("crystallized_water", () -> new Item(new Item.Properties().stacksTo(64).setNoRepair()));

    public static final HashMap<String, RegistryObject<AtomItem>> STABLE_ATOM = new HashMap<>();
    public static final HashMap<String, RegistryObject<AtomItem>> UNSTABLE_ATOM = new HashMap<>();

    public static void atomRegister(String path, String convertedItem, Integer ratio) {
        var item = ITEMS.register("atom_" + path, () -> new AtomItem(convertedItem, false, path, ratio));
        var item0 = ITEMS.register("unstable_magic_atom_" + path, () -> new AtomItem(convertedItem, true, path, ratio));

        items.add(item);
        items.add(item0);

        STABLE_ATOM.put(path, item);
        UNSTABLE_ATOM.put(path, item0);
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

    public static final RegistryObject<PresetCasterTome> ATOM_ASSEMBLY_TOME = ITEMS.register("atom_assembly_tome",
            () -> new PresetCasterTome(() -> new Spell().add(MethodTouch.INSTANCE).add(EffectAssemblyAtom.INSTANCE))
    );

    public static final RegistryObject<PresetCasterTome> ATOM_AGGREGATION_TOME = ITEMS.register("atom_aggregation_tome",
            () -> new PresetCasterTome(() -> new Spell().add(MethodTouch.INSTANCE).add(EffectAggregationAtom.INSTANCE)));

    public static void register(IEventBus ibus) {
        atomRegister("hydrogen", "", 0);
        atomRegister("oxygen", "", 0);
        atomRegister("iron", "minecraft:raw_iron", 56);
        atomRegister("copper", "minecraft:raw_copper", 64);
        atomRegister("coal", "minecraft:coal", 12);

        ITEMS.register(ibus);

    }
}
