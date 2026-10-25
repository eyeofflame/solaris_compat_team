package dev.efm.solaris_core;

import dev.efm.solaris_core.api.multiblock.IMultiblockLogic;
import dev.efm.solaris_core.api.multiblock.MultiblockLogics;
import dev.efm.solaris_core.api.multiblock.SolarisMasterBlockEntity;
import dev.efm.solaris_core.client.multiblock.MultiblockModels;
import dev.efm.solaris_core.common.SolarisRegistry;
import dev.efm.solaris_core.config.SolarisConfigScreen;
import dev.efm.solaris_core.config.SolarisCoreConfig;
import dev.efm.solaris_core.example.SMuti;
import dev.efm.solaris_core.functions.structureReplacement.DataLoader;
import dev.efm.solaris_core.helper.GameHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * SolarisCore：数据驱动能力（战利品替换、结构替换）+ 多方块机器 API。
 * 其余玩法功能在 solaris_functions 子项目。
 */
@Mod(SolarisCore.MODID)
public class SolarisCore {
    public static final String MODID = "solaris_core";

    public SolarisCore(FMLJavaModLoadingContext context) {
        IEventBus ibus = context.getModEventBus();
        IEventBus fbus = MinecraftForge.EVENT_BUS;

        context.registerConfig(ModConfig.Type.COMMON, SolarisCoreConfig.SPEC);
        registerConfigScreen(context);

        SolarisRegistry.register(ibus);

        fbus.addListener(DataLoader::onReload);

        MultiblockLogics.register(SMuti.COKE.getId(), new IMultiblockLogic() {
            @Override
            public void onFormed(ServerLevel level, SolarisMasterBlockEntity master) {
                //IMultiblockLogic.super.onFormed(level, master);
                level.getServer().sendSystemMessage(Component.literal("yes!"));
            }
        });

        MultiblockModels.register(SMuti.COKE.getId(), new MultiblockModels.MultiblockModel(
                GameHelper.buildRes("solaris_core", "geo/multiblock/example.geo.json"),
                GameHelper.buildRes("minecraft", "textures/block/iron_block.png"),
                null));

    }

    /**
     * 只有装了 Cloth Config 才注册配置界面；没装时 mod 仍能正常加载。
     */
    private static void registerConfigScreen(FMLJavaModLoadingContext context) {
        if (FMLEnvironment.dist != Dist.CLIENT || !ModList.get().isLoaded("cloth_config")) {
            return;
        }
        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> SolarisConfigScreen.create(parent)
                )
        );
    }
}
