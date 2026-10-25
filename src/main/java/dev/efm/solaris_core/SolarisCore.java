package dev.efm.solaris_core;

import dev.efm.solaris_core.common.SolarisRegistry;
import dev.efm.solaris_core.functions.structureReplacement.DataLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * SolarisCore：只保留数据驱动的两块能力 —— 战利品替换与结构替换。
 * 其余玩法功能已拆到独立子项目 solaris_functions。
 */
@Mod(SolarisCore.MODID)
public class SolarisCore {
    public static final String MODID = "solaris_core";

    public SolarisCore(FMLJavaModLoadingContext context) {
        IEventBus ibus = context.getModEventBus();
        IEventBus fbus = MinecraftForge.EVENT_BUS;

        SolarisRegistry.register(ibus);

        fbus.addListener(DataLoader::onReload);
    }
}
