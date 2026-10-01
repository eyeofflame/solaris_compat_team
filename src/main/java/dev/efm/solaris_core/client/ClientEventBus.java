package dev.efm.solaris_core.client;

import dev.efm.solaris_core.client.entities.layers.SolarisModelLayer;
import dev.efm.solaris_core.client.entities.models.NuclearEntityModel;
import dev.efm.solaris_core.client.entities.renderers.NuclearEntityRenderer;
import dev.efm.solaris_core.common.SolarisRegistry;
import dev.efm.solaris_core.common.entities.NuclearBombEntity;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientEventBus {
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onClientSetup(FMLClientSetupEvent evt) {
        EntityRenderers.register((EntityType<? extends NuclearBombEntity>) SolarisRegistry.NUCLEAR_BOMB.get(), NuclearEntityRenderer::new);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions evt){
        evt.registerLayerDefinition(SolarisModelLayer.NUCLEAR_BOMB, NuclearEntityModel::createBodyLayer);
    }
}
