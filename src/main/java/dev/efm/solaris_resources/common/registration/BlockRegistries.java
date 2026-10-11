package dev.efm.solaris_resources.common.registration;

import com.simibubi.create.api.stress.BlockStressValues;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.block.MagicGenMachine;
import dev.efm.solaris_resources.common.blockentity.MagicGenEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

public final class BlockRegistries {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, SolarisResources.MODID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SolarisResources.MODID);
    private static final Map<RegistryObject<? extends KineticBlock>, Double> STRESS_IMPACTS = new LinkedHashMap<>();
    private static boolean registered;
    private static boolean stressInitialized;

    public static final RegistryObject<MagicGenMachine> MAGIC_GEN_MACHINE = registerBlock("test0",
            () -> new MagicGenMachine(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
    public static final RegistryObject<BlockEntityType<MagicGenEntity>> MAGIC_GEN_ENTITY = registerBlockEntity(
            "test0", MagicGenEntity::new, MAGIC_GEN_MACHINE);
    public static final RegistryObject<BlockItem> TEST2 = registerBlockItem(MAGIC_GEN_MACHINE, Item.Properties::new);

    static {
        registerStressImpact(MAGIC_GEN_MACHINE, 8d);
    }

    private BlockRegistries() { }

    public static <B extends Block> RegistryObject<B> registerBlock(String name, Supplier<? extends B> factory) {
        return BLOCKS.register(name, Objects.requireNonNull(factory, "block factory"));
    }

    public static <B extends Block> RegistryObject<BlockItem> registerBlockItem(
            RegistryObject<B> block, Supplier<Item.Properties> properties) {
        Objects.requireNonNull(properties, "item properties factory");
        return registerBlockItem(block, value -> new BlockItem(value, properties.get()));
    }

    public static <B extends Block, I extends BlockItem> RegistryObject<I> registerBlockItem(
            RegistryObject<B> block, Function<? super B, ? extends I> factory) {
        Objects.requireNonNull(block, "block");
        Objects.requireNonNull(factory, "item factory");
        if (!SolarisResources.MODID.equals(block.getId().getNamespace())) {
            throw new IllegalArgumentException("Block items must belong to " + SolarisResources.MODID);
        }
        RegistryObject<I> item = ItemRegistries.ITEMS.register(block.getId().getPath(), () -> factory.apply(block.get()));
        CreativeTabRegistries.addItem(item);
        return item;
    }

    @SafeVarargs
    public static <T extends BlockEntity> RegistryObject<BlockEntityType<T>> registerBlockEntity(
            String name, BlockEntityFactory<T> factory, Supplier<? extends Block>... blocks) {
        Objects.requireNonNull(factory, "block entity factory");
        Objects.requireNonNull(blocks, "valid blocks");
        if (blocks.length == 0) throw new IllegalArgumentException("A block entity must have at least one valid block");
        var validBlocks = Arrays.stream(blocks).map(block -> Objects.requireNonNull(block, "valid block")).toList();
        var type = new AtomicReference<RegistryObject<BlockEntityType<T>>>();
        RegistryObject<BlockEntityType<T>> entry = BLOCK_ENTITIES.register(name, () ->
                BlockEntityType.Builder.<T>of((pos, state) -> factory.create(type.get().get(), pos, state),
                        validBlocks.stream().map(Supplier::get).toArray(Block[]::new)).build(null));
        type.set(entry);
        return entry;
    }

    public static void registerStressImpact(RegistryObject<? extends KineticBlock> block, double impact) {
        Objects.requireNonNull(block, "kinetic block");
        if (!Double.isFinite(impact) || impact < 0d) {
            throw new IllegalArgumentException("Stress impact must be finite and non-negative");
        }
        if (stressInitialized) throw new IllegalStateException("Stress declarations are closed after common setup");
        if (STRESS_IMPACTS.putIfAbsent(block, impact) != null) {
            throw new IllegalArgumentException("Duplicate stress impact for " + block.getId());
        }
    }

    public static void register(IEventBus bus) {
        Objects.requireNonNull(bus, "mod event bus");
        if (registered) throw new IllegalStateException("Block registries already attached to the event bus");
        BLOCKS.register(bus);
        BLOCK_ENTITIES.register(bus);
        bus.addListener(BlockRegistries::commonSetup);
        registered = true;
    }

    private static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            STRESS_IMPACTS.forEach((block, impact) -> BlockStressValues.IMPACTS.register(block.get(), () -> impact));
            stressInitialized = true;
        });
    }

    @FunctionalInterface
    public interface BlockEntityFactory<T extends BlockEntity> {
        T create(BlockEntityType<T> type, BlockPos pos, BlockState state);
    }
}
