package dev.efm.solaris_resources;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import dev.efm.solaris_resources.common.registration.CreativeTabRegistries;
import dev.efm.solaris_resources.common.config.AtomConfigs;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import dev.efm.solaris_resources.common.registration.EntityTypeRegistries;
import dev.efm.solaris_resources.common.registration.ItemRegistries;
import dev.efm.solaris_resources.common.spells.EffectAggregationAtom;
import dev.efm.solaris_resources.common.spells.EffectAssemblyAtom;
import dev.efm.solaris_resources.common.spells.EffectConvertWater;
import dev.efm.solaris_resources.common.spells.EffectDissolveWater;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.Random;

@Mod(SolarisResources.MODID)
public class SolarisResources {

    public static final String MODID = "solaris_resources";

    public static final Random RND = new Random();

    public SolarisResources(FMLJavaModLoadingContext context) {
        IEventBus ibus = context.getModEventBus(), fbus = MinecraftForge.EVENT_BUS;

        ItemRegistries.register(ibus);
        CreativeTabRegistries.register(ibus);
        EntityTypeRegistries.register(ibus);
        ibus.addListener(this::commonSetup);

        fbus.addListener(ItemRegistries::itemTip);

        registerSpell();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> AtomConfigs.STORE.reload());
    }

    private static void registerSpell() {
        GlyphRegistry.registerSpell(EffectConvertWater.INSTANCE);
        GlyphRegistry.registerSpell(EffectDissolveWater.INSTANCE);
        GlyphRegistry.registerSpell(EffectAggregationAtom.INSTANCE);
        GlyphRegistry.registerSpell(EffectAssemblyAtom.INSTANCE);
    }
}
