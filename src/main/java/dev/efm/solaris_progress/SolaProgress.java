package dev.efm.solaris_progress;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SolaProgress.MODID)
public class SolaProgress {
    public static final String MODID = "solaris_progress";

    public SolaProgress(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.register(ForgeEvents.class);
        MinecraftForge.EVENT_BUS.register(SettlementProtection.class);
        FTBEvents.init();
        context.registerConfig(ModConfig.Type.COMMON, SolaConfig.SPEC);
        context.getModEventBus().addListener(SolaConfig::onLoad);
    }
}
