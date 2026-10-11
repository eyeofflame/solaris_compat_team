package dev.efm.solaris_resources.common.registration;

import com.simibubi.create.api.stress.BlockStressValues;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.block.MagicGenMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

@GameTestHolder(SolarisResources.MODID)
@PrefixGameTestTemplate(false)
@Mod.EventBusSubscriber(modid = SolarisResources.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class BlockRegistriesGameTests {
    // Declare fixtures before the normal DeferredRegister handlers, never from the running test body.
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void registerFixtures(RegisterEvent event) {
        if (event.getRegistryKey().equals(ForgeRegistries.Keys.BLOCKS)) {
            Fixtures.initialize();
        }
    }

    @GameTest(template = "empty")
    public static void blockItemsRetainFactoriesAndIds(GameTestHelper helper) {
        helper.assertTrue(Fixtures.BLOCK_CALLS == 2, "block factories must run once per registered block");
        helper.assertTrue(Fixtures.ITEM_CALLS == 1, "custom item factory must run once after block registration");
        helper.assertTrue(Fixtures.DEFAULT_ITEM.get().getBlock() == Fixtures.FIRST.get(), "default item must bind its block");
        helper.assertTrue(Fixtures.DEFAULT_ITEM.get().getMaxStackSize() == 16, "default item properties must be honored");
        helper.assertTrue(Fixtures.CUSTOM_ITEM.get().getBlock() == Fixtures.SECOND.get(), "custom item must bind its block");
        helper.assertTrue(Fixtures.CUSTOM_ITEM.get().getMaxStackSize() == 1, "custom item factory properties must be honored");
        helper.assertTrue(Fixtures.DEFAULT_ITEM.getId().equals(Fixtures.FIRST.getId()), "item and block IDs must match");
        helper.assertTrue(Fixtures.CUSTOM_ITEM.getId().equals(Fixtures.SECOND.getId()), "custom item ID must match block ID");
        helper.assertTrue(ForgeRegistries.ITEMS.containsKey(Fixtures.CUSTOM_ITEM.getId()), "items must use shared item registry");
        helper.assertFalse(ForgeRegistries.ITEMS.containsKey(Fixtures.NO_ITEM.getId()), "block-only registration must not create an item");
        var tab = CreativeTabRegistries.SOLARIS_RESOURCES.get();
        var parameters = new CreativeModeTab.ItemDisplayParameters(
                helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess());
        tab.buildContents(parameters);
        var entries = tab.getDisplayItems();
        helper.assertTrue(entries.stream().anyMatch(stack -> stack.is(Fixtures.DEFAULT_ITEM.get())), "default block item must be added to the Solaris tab");
        helper.assertTrue(entries.stream().anyMatch(stack -> stack.is(Fixtures.CUSTOM_ITEM.get())), "custom block item must be added to the Solaris tab");
        helper.assertTrue(entries.stream().anyMatch(stack -> stack.is(BlockRegistries.TEST2.get())), "existing registered block item must be added to the Solaris tab");
        helper.assertTrue(entries.stream().anyMatch(stack -> stack.is(ItemRegistries.CRYSTALLIZED_WATER.get())), "existing creative tab contents must remain");
        helper.assertTrue(entries.stream().noneMatch(stack -> stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() == Fixtures.NO_ITEM.get()), "block-only registration must not appear in the tab");
        CreativeTabRegistries.addItem(Fixtures.DEFAULT_ITEM);
        CreativeTabRegistries.addItem(Fixtures.CUSTOM_ITEM);
        tab.buildContents(parameters);
        helper.assertTrue(tab.getDisplayItems().stream().filter(stack -> stack.is(Fixtures.DEFAULT_ITEM.get())).count() == 1,
                "repeated default item declarations must not duplicate tab contents");
        helper.assertTrue(tab.getDisplayItems().stream().filter(stack -> stack.is(Fixtures.CUSTOM_ITEM.get())).count() == 1,
                "repeated custom item declarations must not duplicate tab contents");
        helper.assertTrue(tab.getSearchTabDisplayItems().stream().anyMatch(stack -> stack.is(Fixtures.CUSTOM_ITEM.get())),
                "automatic items must also be searchable");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void entityFactoryReceivesSharedType(GameTestHelper helper) {
        var type = Fixtures.ENTITY.get();
        for (Block block : new Block[]{Fixtures.FIRST.get(), Fixtures.SECOND.get()}) {
            helper.assertTrue(type.isValid(block.defaultBlockState()), "each declared block must be valid for the shared entity");
            var entity = type.create(BlockPos.ZERO, block.defaultBlockState());
            helper.assertTrue(entity != null && entity.getType() == type, "factory must receive the registered type itself");
        }
        helper.assertFalse(type.isValid(Blocks.DIRT.defaultBlockState()), "unrelated blocks must not become valid");
        helper.assertTrue(BlockStressValues.getImpact(Fixtures.KINETIC.get()) == 12d, "declared impact must be installed in common setup");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void invalidDeclarationsFailBeforeResolution(GameTestHelper helper) {
        helper.assertTrue(Fixtures.INVALID_CHECKS_PASSED, "invalid and duplicate declarations must fail during declaration, not resolution");
        helper.succeed();
    }

    private static final class Fixtures {
        private static int BLOCK_CALLS;
        private static int ITEM_CALLS;
        private static final RegistryObject<ProbeBlock> FIRST = BlockRegistries.registerBlock("registration_probe_first", () -> {
            BLOCK_CALLS++;
            return new ProbeBlock();
        });
        private static final RegistryObject<ProbeBlock> SECOND = BlockRegistries.registerBlock("registration_probe_second", () -> {
            BLOCK_CALLS++;
            return new ProbeBlock();
        });
        private static final RegistryObject<Block> NO_ITEM = BlockRegistries.registerBlock("registration_probe_no_item",
                () -> new Block(BlockBehaviour.Properties.copy(Blocks.STONE)));
        private static final RegistryObject<MagicGenMachine> KINETIC = BlockRegistries.registerBlock("registration_probe_kinetic",
                () -> new MagicGenMachine(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
        private static final RegistryObject<BlockItem> DEFAULT_ITEM = BlockRegistries.registerBlockItem(FIRST,
                () -> new Item.Properties().stacksTo(16));
        private static final RegistryObject<ProbeItem> CUSTOM_ITEM = BlockRegistries.registerBlockItem(SECOND, block -> {
            ITEM_CALLS++;
            return new ProbeItem(block);
        });
        private static final RegistryObject<BlockEntityType<ProbeEntity>> ENTITY = BlockRegistries.registerBlockEntity(
                "registration_probe_shared", ProbeEntity::new, FIRST, SECOND);
        private static final boolean INVALID_CHECKS_PASSED;

        static {
            if (BLOCK_CALLS != 0 || ITEM_CALLS != 0) throw new AssertionError("registration helpers must not resolve factories eagerly");
            expectFailure(IllegalArgumentException.class, () -> BlockRegistries.registerBlockEntity("registration_probe_empty", ProbeEntity::new));
            expectFailure(NullPointerException.class, () -> BlockRegistries.registerBlock("registration_probe_null", null));
            expectFailure(IllegalArgumentException.class, () -> BlockRegistries.registerBlock("registration_probe_first", ProbeBlock::new));
            expectFailure(IllegalArgumentException.class, () -> BlockRegistries.registerBlockItem(FIRST, () -> new Item.Properties()));
            for (double invalid : new double[]{-1d, Double.NaN, Double.POSITIVE_INFINITY}) {
                expectFailure(IllegalArgumentException.class, () -> BlockRegistries.registerStressImpact(KINETIC, invalid));
            }
            BlockRegistries.registerStressImpact(KINETIC, 12d);
            expectFailure(IllegalArgumentException.class, () -> BlockRegistries.registerStressImpact(KINETIC, 13d));
            INVALID_CHECKS_PASSED = true;
        }

        private static void initialize() { }

        private static void expectFailure(Class<? extends RuntimeException> expected, Runnable action) {
            try {
                action.run();
            } catch (RuntimeException failure) {
                if (expected.isInstance(failure)) return;
                throw failure;
            }
            throw new AssertionError("expected " + expected.getSimpleName());
        }
    }

    public static final class ProbeBlock extends Block {
        public ProbeBlock() { super(BlockBehaviour.Properties.copy(Blocks.STONE)); }
    }

    public static final class ProbeItem extends BlockItem {
        public ProbeItem(ProbeBlock block) { super(block, new Item.Properties().stacksTo(1)); }
    }

    public static final class ProbeEntity extends BlockEntity {
        public ProbeEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) { super(type, pos, state); }
    }
}
