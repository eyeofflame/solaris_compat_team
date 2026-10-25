package dev.efm.solaris_functions.common;

import dev.efm.solaris_functions.SolarisFunctions;
import dev.efm.solaris_functions.common.entities.NuclearBombEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * solaris_functions 自己的注册表：核弹实体与刷怪蛋。
 * 战利品修改器 / 结构替换的注册仍留在 solaris_core。
 */
public class SolarisFunctionsRegistry {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, SolarisFunctions.MODID);
    public static final RegistryObject<EntityType<NuclearBombEntity>> NUCLEAR_BOMB = ENTITY_TYPES.register("nuclear_bomb", () -> EntityType.Builder.of(NuclearBombEntity::new, MobCategory.MISC).sized(1f, 1.5f).build("nuclear_bomb"));

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, SolarisFunctions.MODID);
    public static final RegistryObject<ForgeSpawnEggItem> NUCLEAR_BOMB_SPAWN_EGG = ITEMS.register("nuclear_bomb_spawn_egg", () -> new ForgeSpawnEggItem(NUCLEAR_BOMB, 0xcf5656, 0xdb3a3a, new Item.Properties().stacksTo(64)));

    public static void register(IEventBus ibus) {
        ENTITY_TYPES.register(ibus);
        ITEMS.register(ibus);
    }
}
