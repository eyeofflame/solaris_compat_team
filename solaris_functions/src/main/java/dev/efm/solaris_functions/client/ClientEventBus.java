package dev.efm.solaris_functions.client;

import dev.efm.solaris_functions.client.entities.layers.SolarisModelLayer;
import dev.efm.solaris_functions.client.entities.models.NuclearEntityModel;
import dev.efm.solaris_functions.client.entities.renderers.NuclearEntityRenderer;
import dev.efm.solaris_functions.common.SolarisFunctionsRegistry;
import dev.efm.solaris_functions.common.entities.NuclearBombEntity;
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
        EntityRenderers.register((EntityType<? extends NuclearBombEntity>) SolarisFunctionsRegistry.NUCLEAR_BOMB.get(), NuclearEntityRenderer::new);
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions evt){
        evt.registerLayerDefinition(SolarisModelLayer.NUCLEAR_BOMB, NuclearEntityModel::createBodyLayer);
    }
}
