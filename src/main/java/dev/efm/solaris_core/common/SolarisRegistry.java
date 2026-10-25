package dev.efm.solaris_core.common;

import com.mojang.serialization.Codec;
import dev.efm.solaris_core.SolarisCore;
import dev.efm.solaris_core.api.multiblock.SolarisDummyBlockEntity;
import dev.efm.solaris_core.api.multiblock.SolarisMasterBlockEntity;
import dev.efm.solaris_core.api.multiblock.SolarisPartBlock;
import dev.efm.solaris_core.functions.lootItemReplace.SolarisModifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class SolarisRegistry {

    /** 战利品修改器序列化器（沿用 solaris 命名空间，数据文件在 data/solaris/）。 */
    public static final DeferredRegister<Codec<? extends IGlobalLootModifier>> CODEC_DEFERRED_REGISTER = DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, "solaris");
    public static final RegistryObject<Codec<SolarisModifier>> CODEC_REGISTRY_OBJECT = CODEC_DEFERRED_REGISTER.register("loot_replacement", () -> SolarisModifier.CODEC);

    /** 多方块部件方块（master/slave 共用一个方块，用 slave 属性区分）。 */
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SolarisCore.MODID);
    public static final RegistryObject<SolarisPartBlock> PART_BLOCK = BLOCKS.register("part",
            () -> new SolarisPartBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(3.0F, 6.0F)
                    .noOcclusion()));

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SolarisCore.MODID);
    public static final RegistryObject<BlockEntityType<SolarisMasterBlockEntity>> PART_MASTER_BE = BLOCK_ENTITIES.register("part_master",
            () -> BlockEntityType.Builder.of(SolarisMasterBlockEntity::new, PART_BLOCK.get()).build(null));
    public static final RegistryObject<BlockEntityType<SolarisDummyBlockEntity>> PART_DUMMY_BE = BLOCK_ENTITIES.register("part_dummy",
            () -> BlockEntityType.Builder.of(SolarisDummyBlockEntity::new, PART_BLOCK.get()).build(null));

    public static void register(IEventBus ibus) {
        CODEC_DEFERRED_REGISTER.register(ibus);
        BLOCKS.register(ibus);
        BLOCK_ENTITIES.register(ibus);
    }
}
