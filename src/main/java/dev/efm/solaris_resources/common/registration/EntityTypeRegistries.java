package dev.efm.solaris_resources.common.registration;

import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.entity.AtomItemEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class EntityTypeRegistries {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SolarisResources.MODID);

    public static final RegistryObject<EntityType<AtomItemEntity>> ATOM_ITEM =
            ENTITY_TYPES.register("atom_item", () -> EntityType.Builder
                    .<AtomItemEntity>of(AtomItemEntity::new, MobCategory.MISC)
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(6)
                    .updateInterval(20)
                    .build("atom_item"));

    public static void register(IEventBus bus) {
        ENTITY_TYPES.register(bus);
    }
}
