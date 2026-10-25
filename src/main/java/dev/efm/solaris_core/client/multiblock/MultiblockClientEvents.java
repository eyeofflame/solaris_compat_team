package dev.efm.solaris_core.client.multiblock;

import dev.efm.solaris_core.SolarisCore;
import dev.efm.solaris_core.common.SolarisRegistry;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 客户端注册：master 的 GeckoLib 渲染器。
 * 模型本身由 GeckoLib 按 {@link MultiblockModels} 里的路径自动加载，不需要 ModelEvent。
 */
@Mod.EventBusSubscriber(modid = SolarisCore.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MultiblockClientEvents {

    private MultiblockClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(SolarisRegistry.PART_MASTER_BE.get(), SolarisMultiblockRenderer::new);
    }
}
