package dev.efm.solaris_compat.client;

import dev.efm.solaris_compat.SolarisCompat;
import dev.efm.solaris_compat.common.entity.OliviaEntities;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SolarisCompat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class OliviaClientEvents {

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(OliviaEntities.OLIVIA, OliviaRenderer::new);
    }
}