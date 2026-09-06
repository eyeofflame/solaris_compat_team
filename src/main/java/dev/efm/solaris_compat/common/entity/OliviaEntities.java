package dev.efm.solaris_compat.common.entity;

import dev.efm.solaris_compat.SolarisCompat;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = SolarisCompat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class OliviaEntities {

    public static EntityType<OliviaEntity> OLIVIA;

    @SubscribeEvent
    public static void onRegisterEntities(RegisterEvent event) {
        event.register(ForgeRegistries.Keys.ENTITY_TYPES, helper -> {
            OLIVIA = EntityType.Builder.of(OliviaEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 1.8F)
                    .build("olivia");
            helper.register(ResourceLocation.fromNamespaceAndPath(SolarisCompat.MODID, "olivia"), OLIVIA);
        });
    }

    @SubscribeEvent
    public static void onRegisterAttributes(EntityAttributeCreationEvent event) {
        event.put(OLIVIA, OliviaEntity.createAttributes().build());
    }
}