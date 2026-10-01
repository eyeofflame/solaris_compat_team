package dev.efm.solaris_core.common;

import com.mojang.serialization.Codec;
import dev.efm.solaris_core.SolarisCore;
import dev.efm.solaris_core.common.effects.SMobEffectsRegister;
import dev.efm.solaris_core.common.entities.NuclearBombEntity;
import dev.efm.solaris_core.common.potions.SPotionsRegister;
import dev.efm.solaris_core.functions.lootItemReplace.SolarisModifier;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SolarisRegistry {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> CODEC_DEFERRED_REGISTER = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "solaris");
    public static final RegistryObject<Codec<SolarisModifier>> CODEC_REGISTRY_OBJECT = CODEC_DEFERRED_REGISTER.register("loot_replacement", () -> SolarisModifier.CODEC);

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SolarisCore.MODID);
    public static final RegistryObject<EntityType<NuclearBombEntity>> NUCLEAR_BOMB = ENTITY_TYPES.register("nuclear_bomb", () -> EntityType.Builder.of(NuclearBombEntity::new, MobCategory.MISC).sized(1f, 1.5f).build("nuclear_bomb"));

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SolarisCore.MODID);
    public static final RegistryObject<ForgeSpawnEggItem> NUCLEAR_BOMB_SPAWN_EGG = ITEMS.register("nuclear_bomb_spawn_egg", () -> new ForgeSpawnEggItem(NUCLEAR_BOMB, 0xcf5656, 0xdb3a3a, new Item.Properties().stacksTo(64)));

    public static void register(IEventBus ibus) {
        SMobEffectsRegister.register(ibus);
        SPotionsRegister.register(ibus);
        CODEC_DEFERRED_REGISTER.register(ibus);
        ENTITY_TYPES.register(ibus);
        ITEMS.register(ibus);
    }
}
