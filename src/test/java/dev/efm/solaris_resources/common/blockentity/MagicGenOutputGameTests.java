package dev.efm.solaris_resources.common.blockentity;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.registration.BlockRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(SolarisResources.MODID)
@PrefixGameTestTemplate(false)
public class MagicGenOutputGameTests {

    private static final ResourceLocation FLUID_TANK = ResourceLocation.fromNamespaceAndPath("create", "fluid_tank");

    /** addOutput 的物品种类与数量校验。 */
    @GameTest(template = "empty")
    public static void addOutputValidatesRequest(GameTestHelper helper) {
        MagicGenEntity core = placeMachine(helper, new BlockPos(2, 2, 2));

        helper.assertFalse(core.addOutput(Items.AIR, 1), "air must be rejected");
        helper.assertFalse(core.addOutput(Items.STONE, 0), "zero count must be rejected");
        helper.assertFalse(core.addOutput(Items.STONE, -5), "negative count must be rejected");
        helper.assertFalse(core.addOutput(Items.STONE, MagicGenEntity.MAX_OUTPUT_PER_CALL + 1),
                "more than 64 in one call must be rejected");
        helper.assertFalse(core.addOutput(ItemStack.EMPTY), "an empty stack must be rejected");
        helper.assertTrue(core.getOutput(0).isEmpty(), "a rejected request must not touch the slots");

        helper.assertTrue(core.addOutput(Items.STONE, 64), "64 stone must fit");
        helper.assertTrue(core.getOutput(0).getCount() == 64, "stone must land in the first slot");

        // 第一个槽满了不代表整台机器收不了同类物品——空槽仍然可以接。
        helper.assertTrue(core.addOutput(Items.STONE, 1), "an empty slot can still take the same item");
        helper.assertTrue(core.getOutput(1).getCount() == 1, "the spare stone must land in the second slot");
        helper.assertTrue(core.addOutput(Items.DIRT, 10), "a different item must go to the next free slot");
        helper.assertTrue(core.getOutput(2).is(Items.DIRT) && core.getOutput(2).getCount() == 10,
                "dirt must land in the third slot");
        helper.succeed();
    }

    /** 同一个槽只装一种物品，整批放不下时一个都不放。 */
    @GameTest(template = "empty")
    public static void addOutputIsAllOrNothing(GameTestHelper helper) {
        MagicGenEntity core = placeMachine(helper, new BlockPos(2, 2, 2));
        helper.assertTrue(core.addOutput(Items.STONE, 60), "60 stone must fit");
        helper.assertTrue(core.addOutput(Items.DIRT, 64), "dirt must take the second slot");
        helper.assertTrue(core.addOutput(Items.COBBLESTONE, 64), "cobblestone must take the third slot");

        helper.assertTrue(core.countOutputSpace(new ItemStack(Items.STONE)) == 4,
                "only 4 stone can still fit, got " + core.countOutputSpace(new ItemStack(Items.STONE)));
        helper.assertFalse(core.addOutput(Items.STONE, 5), "5 must not fit as a whole batch");
        helper.assertTrue(core.getOutput(0).getCount() == 60, "the rejected batch must not be half-applied");

        helper.assertTrue(core.addOutput(Items.STONE, 4), "4 must fill the first slot exactly");
        helper.assertTrue(core.getOutput(0).getCount() == 64, "the first slot must be full now");
        helper.assertFalse(core.addOutput(Items.STONE, 1), "no slot can take more stone");
        helper.succeed();
    }

    /** insertOutput 是"尽量塞"版本，返回余量。 */
    @GameTest(template = "empty")
    public static void insertOutputReturnsLeftover(GameTestHelper helper) {
        MagicGenEntity core = placeMachine(helper, new BlockPos(2, 2, 2));
        ItemStack leftover = core.insertOutput(new ItemStack(Items.STONE, 200));
        helper.assertTrue(leftover.getCount() == 200 - 3 * MagicGenEntity.OUTPUT_SLOT_LIMIT,
                "3 slots must take 192 of the 200, leftover " + leftover.getCount());
        helper.assertTrue(core.getOutput(0).getCount() == 64 && core.getOutput(1).getCount() == 64
                && core.getOutput(2).getCount() == 64, "all three slots must be full");
        helper.succeed();
    }

    /** 任意一个槽满了，机器就不再处理；腾出空位后自动恢复。 */
    @GameTest(template = "empty")
    public static void fullOutputSlotStopsProcessing(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MagicGenEntity core = placeMachineOverTank(helper, pos);
        tankBelow(helper, pos).fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        core.setSource(100);
        helper.assertTrue(core.addOutput(Items.STONE, 64), "the first slot must be filled to the brim");

        for (int i = 0; i < 40; i++) {
            helper.assertFalse(core.tickProcessing(MagicGenEntity.FAST_RPM),
                    "a full output slot must stop processing");
        }
        helper.assertTrue(core.getCompletedProcesses() == 0, "nothing may be processed while blocked");
        helper.assertTrue(core.getProgress() == 0.0F, "progress must not advance while blocked");
        helper.assertTrue(core.getSource() == 100, "no source may be consumed while blocked");

        core.getOutputs().extractItem(0, 1, false, false);
        for (int i = 0; i < MagicGenEntity.TICKS_AT_FAST_RPM; i++) {
            core.tickProcessing(MagicGenEntity.FAST_RPM);
        }
        helper.assertTrue(core.getCompletedProcesses() == 1, "freeing a slot must resume processing");
        helper.succeed();
    }

    /** 输出内容要能过存档往返。 */
    @GameTest(template = "empty")
    public static void outputsSurviveSaveAndLoad(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        BlockPos absolute = helper.absolutePos(pos);
        MagicGenEntity core = placeMachine(helper, pos);
        helper.assertTrue(core.addOutput(Items.STONE, 32), "32 stone must fit");
        helper.assertTrue(core.addOutput(Items.DIRT, 7), "7 dirt must fit");

        CompoundTag saved = core.saveWithoutMetadata();
        MagicGenEntity restored = BlockRegistries.MAGIC_GEN_ENTITY.get()
                .create(absolute, helper.getLevel().getBlockState(absolute));
        helper.assertTrue(restored != null, "the registered block entity type must create a machine");
        restored.setLevel(helper.getLevel());
        restored.load(saved);

        helper.assertTrue(restored.getOutput(0).is(Items.STONE) && restored.getOutput(0).getCount() == 32,
                "the first slot must round-trip");
        helper.assertTrue(restored.getOutput(1).is(Items.DIRT) && restored.getOutput(1).getCount() == 7,
                "the second slot must round-trip");
        helper.succeed();
    }

    /** 外部物流（漏斗/管道/装卸器）能抽走产物，但塞不进来。 */
    @GameTest(template = "empty")
    public static void externalLogisticsCanOnlyExtract(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MagicGenEntity core = placeMachine(helper, pos);
        helper.assertTrue(core.addOutput(Items.STONE, 10), "10 stone must fit");

        IItemHandler handler = core.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).orElse(null);
        helper.assertTrue(handler != null, "the machine must expose an item handler to logistics");
        helper.assertTrue(handler.getSlots() == MagicGenEntity.OUTPUT_SLOTS, "all three output slots must be visible");

        ItemStack pulled = handler.extractItem(0, 4, false);
        helper.assertTrue(pulled.is(Items.STONE) && pulled.getCount() == 4, "a pipe must be able to pull 4 stone");
        helper.assertTrue(core.getOutput(0).getCount() == 6, "the storage must drop to 6");

        ItemStack rejected = handler.insertItem(0, new ItemStack(Items.DIRT, 8), false);
        helper.assertTrue(rejected.getCount() == 8, "external devices must not be able to push items in");
        helper.assertTrue(core.getOutput(0).getCount() == 6, "the rejected insert must not change the slot");

        helper.assertTrue(handler.extractItem(0, 64, true).getCount() == 6, "simulated pulls must not consume");
        helper.assertTrue(core.getOutput(0).getCount() == 6, "the simulated pull must leave the slot alone");
        helper.succeed();
    }

    /** 界面里的三个 sola_output_* 槽位必须真的接到输出存储。 */
    @GameTest(template = "empty")
    public static void uiBindsOutputSlots(GameTestHelper helper) {
        MagicGenEntity core = placeMachine(helper, new BlockPos(2, 2, 2));
        helper.assertTrue(core.addOutput(Items.STONE, 5), "5 stone must fit");

        ModularUI ui = core.createUI(helper.makeMockPlayer());
        for (int slot = 0; slot < MagicGenEntity.OUTPUT_SLOTS; slot++) {
            var widget = ui.mainGroup.getFirstWidgetById(MagicGenEntity.OUTPUT_SLOT_ID_PREFIX + slot);
            helper.assertTrue(widget instanceof SlotWidget,
                    "magic_core_i.ui must expose " + MagicGenEntity.OUTPUT_SLOT_ID_PREFIX + slot
                            + " as an item slot, got " + widget);
        }

        SlotWidget first = (SlotWidget) ui.mainGroup
                .getFirstWidgetById(MagicGenEntity.OUTPUT_SLOT_ID_PREFIX + "0");
        helper.assertTrue(first.getItem().is(Items.STONE) && first.getItem().getCount() == 5,
                "the first slot must show what the storage holds, got " + first.getItem());
        helper.succeed();
    }

    /** 界面里三个槽位都被删掉时，createUI 也不能崩。 */
    @GameTest(template = "empty")
    public static void missingOutputSlotsAreTolerated(GameTestHelper helper) {
        MagicGenEntity core = placeMachine(helper, new BlockPos(2, 2, 2));
        ModularUI ui = core.createUI(helper.makeMockPlayer());
        helper.assertTrue(ui != null, "the ui must still build when the editor has no slots");
        helper.succeed();
    }

    private static MagicGenEntity placeMachine(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, BlockRegistries.MAGIC_GEN_MACHINE.get());
        MagicGenEntity core = (MagicGenEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (core == null) {
            throw new IllegalStateException("the machine block entity must exist");
        }
        return core;
    }

    private static MagicGenEntity placeMachineOverTank(GameTestHelper helper, BlockPos pos) {
        Block tank = ForgeRegistries.BLOCKS.getValue(FLUID_TANK);
        if (tank == null) {
            throw new IllegalStateException("create:fluid_tank is missing");
        }
        helper.setBlock(pos.below(), tank);
        return placeMachine(helper, pos);
    }

    private static IFluidHandler tankBelow(GameTestHelper helper, BlockPos pos) {
        var tankEntity = helper.getLevel().getBlockEntity(helper.absolutePos(pos.below()));
        if (tankEntity == null) {
            throw new IllegalStateException("the tank block entity must exist");
        }
        return tankEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, net.minecraft.core.Direction.UP)
                .orElseThrow(() -> new IllegalStateException("the tank must expose a fluid handler"));
    }
}
