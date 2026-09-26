package dev.efm.mekanism_agriculture;

import dev.efm.mekanism_agriculture.client.ClientSetup;
import dev.efm.mekanism_agriculture.common.config.MekAgriConfig;
import dev.efm.mekanism_agriculture.common.registration.MekBlockTypes;
import dev.efm.mekanism_agriculture.common.registration.MekBlocks;
import dev.efm.mekanism_agriculture.common.registration.MekContainerTypes;
import dev.efm.mekanism_agriculture.common.registration.TileEntityTypes;
import mekanism.common.registration.impl.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MekanismAgriculture.MODID)
public class MekanismAgriculture {
    public static final String MODID = "mekanism_agriculture";

    public static final BlockDeferredRegister BLOCKS = new BlockDeferredRegister(MODID);
    public static final ItemDeferredRegister ITEMS = new ItemDeferredRegister(MODID);
    public static final TileEntityTypeDeferredRegister TILE_ENTITY_TYPES = new TileEntityTypeDeferredRegister(MODID);
    public static final ContainerTypeDeferredRegister CONTAINER_TYPES = new ContainerTypeDeferredRegister(MODID);
    public static final SoundEventDeferredRegister SOUND_EVENTS = new SoundEventDeferredRegister(MODID);

    public MekanismAgriculture(FMLJavaModLoadingContext context) {
        IEventBus iBus = context.getModEventBus();

        context.registerConfig(ModConfig.Type.COMMON, MekAgriConfig.SPEC);

        TileEntityTypes.init();
        MekBlocks.init();
        MekBlockTypes.init();
        MekContainerTypes.init();

        {
            BLOCKS.register(iBus);
            ITEMS.register(iBus);
            TILE_ENTITY_TYPES.register(iBus);
            CONTAINER_TYPES.register(iBus);
            SOUND_EVENTS.register(iBus);
            iBus.addListener(ClientSetup::clientSetup);
        }

    }
}
