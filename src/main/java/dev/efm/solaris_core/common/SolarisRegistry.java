package dev.efm.solaris_core.common;

import com.mojang.serialization.Codec;
import dev.efm.solaris_core.functions.lootItemReplace.SolarisModifier;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * solaris_core 的注册表：仅战利品修改器序列化器。
 */
public class SolarisRegistry {
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> CODEC_DEFERRED_REGISTER = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "solaris");
    public static final RegistryObject<Codec<SolarisModifier>> CODEC_REGISTRY_OBJECT = CODEC_DEFERRED_REGISTER.register("loot_replacement", () -> SolarisModifier.CODEC);

    public static void register(IEventBus ibus) {
        CODEC_DEFERRED_REGISTER.register(ibus);
    }
}
