package dev.efm.solaris_resources.client;

import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.item.AtomItem;
import dev.efm.solaris_resources.common.registration.EntityTypeRegistries;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, modid = SolarisResources.MODID, value = Dist.CLIENT)
public class AtomTextureModelRedirector {
    private static final ModelResourceLocation SHARED = new ModelResourceLocation(ResourceLocation.fromNamespaceAndPath(SolarisResources.MODID, "atom"), "inventory");

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers evt) {
        evt.registerEntityRenderer(EntityTypeRegistries.ATOM_ITEM.get(), AtomItemEntityRenderer::new);
    }

    @SubscribeEvent
    public static void registerAdditional(ModelEvent.RegisterAdditional evt) {
        evt.register(SHARED);
    }

    @SubscribeEvent
    public static void redirect(ModelEvent.ModifyBakingResult evt) {
        BakedModel shared = evt.getModels().get(SHARED);
        if (shared == null) return;
        for (Item item : ForgeRegistries.ITEMS) {
            if (item instanceof AtomItem) {
                ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
                evt.getModels().put(new ModelResourceLocation(id, "inventory"), shared);
            }
        }
    }
}
