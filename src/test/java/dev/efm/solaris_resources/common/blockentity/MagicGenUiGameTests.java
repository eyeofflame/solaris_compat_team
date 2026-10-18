package dev.efm.solaris_resources.common.blockentity;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.TankWidget;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.client.gui.SourceSlotWidget;
import dev.efm.solaris_resources.common.block.MagicGenMachine;
import dev.efm.solaris_resources.common.registration.BlockRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@GameTestHolder(SolarisResources.MODID)
@PrefixGameTestTemplate(false)
public class MagicGenUiGameTests {

    private static final ResourceLocation FLUID_TANK = ResourceLocation.fromNamespaceAndPath("create", "fluid_tank");

    /** 没有储罐时右键不开门。 */
    @GameTest(template = "empty")
    public static void openingNeedsTankBelow(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos, BlockRegistries.MAGIC_GEN_MACHINE.get());

        BlockState vertical = helper.getLevel().getBlockState(absolute);
        helper.assertTrue(vertical.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y,
                "a placed machine must default to a vertical shaft");
        helper.assertFalse(MagicGenMachine.hasFluidTankBelow(helper.getLevel(), absolute),
                "an empty block below must not be treated as a tank");
        helper.assertFalse(MagicGenMachine.canOpenUI(vertical, helper.getLevel(), absolute),
                "without a tank below the ui must stay closed");
        helper.succeed();
    }

    /** 竖直 + 正下方铜流体储罐才开；躺着的机器永远不开。 */
    @GameTest(template = "empty")
    public static void openingNeedsVerticalAxis(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos.below(), tank());
        helper.setBlock(pos, BlockRegistries.MAGIC_GEN_MACHINE.get());

        BlockState vertical = helper.getLevel().getBlockState(absolute);
        helper.assertTrue(MagicGenMachine.hasFluidTankBelow(helper.getLevel(), absolute),
                "the tank below must be recognised");
        helper.assertTrue(MagicGenMachine.canOpenUI(vertical, helper.getLevel(), absolute),
                "vertical machine over a tank must open the ui");

        BlockState sideways = vertical.setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        helper.assertFalse(MagicGenMachine.canOpenUI(sideways, helper.getLevel(), absolute),
                "a horizontal shaft must never open the ui");
        helper.succeed();
    }

    /** 旋转/镜像不允许把轴改成水平。 */
    @GameTest(template = "empty")
    public static void rotationKeepsShaftVertical(GameTestHelper helper) {
        MagicGenMachine machine = BlockRegistries.MAGIC_GEN_MACHINE.get();
        BlockState vertical = machine.defaultBlockState();
        for (Rotation rotation : Rotation.values()) {
            helper.assertTrue(machine.rotate(vertical, rotation).getValue(BlockStateProperties.AXIS) == Direction.Axis.Y,
                    "rotation " + rotation + " must keep the shaft vertical");
        }
        helper.succeed();
    }

    /** 打开 UI 时：模板里必须真的有魔源槽，并且流体槽要接到下方储罐。 */
    @GameTest(template = "empty")
    public static void createUiBindsTanksAndKeepsSourceSlot(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        BlockPos absolute = helper.absolutePos(pos);
        helper.setBlock(pos.below(), tank());
        helper.setBlock(pos, BlockRegistries.MAGIC_GEN_MACHINE.get());

        MagicGenEntity core = (MagicGenEntity) helper.getLevel().getBlockEntity(absolute);
        helper.assertTrue(core != null, "the machine block entity must exist");

        ModularUI ui = core.createUI(helper.makeMockPlayer());
        List<SourceSlotWidget> sources = ui.mainGroup.getWidgetsByType(SourceSlotWidget.class);
        helper.assertTrue(!sources.isEmpty(), "magic_core_i.ui must contain a source_slot widget");

        List<TankWidget> tanks = ui.mainGroup.getWidgetsByType(TankWidget.class);
        helper.assertTrue(!tanks.isEmpty(), "magic_core_i.ui must contain a fluid_slot widget");
        for (TankWidget tank : tanks) {
            helper.assertTrue(tank.getFluidTank() != null, "every fluid slot must be bound to the tank below");
        }
        for (SourceSlotWidget slot : sources) {
            helper.assertTrue(slot.getLastMaxSource() == MagicGenEntity.DEFAULT_MAX_SOURCE,
                    "the source slot must bind to the machine's 800 source storage, got " + slot.getLastMaxSource());
        }
        helper.succeed();
    }

    /** 魔源存储：默认 800 上限，加减都夹在 [0, 800]。 */
    @GameTest(template = "empty")
    public static void sourceStorageClampsAtEightHundred(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, BlockRegistries.MAGIC_GEN_MACHINE.get());
        MagicGenEntity core = (MagicGenEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(core != null, "the machine block entity must exist");

        helper.assertTrue(core.getMaxSource() == MagicGenEntity.DEFAULT_MAX_SOURCE,
                "the machine must hold 800 source, got " + core.getMaxSource());
        helper.assertTrue(core.getTransferRate() == MagicGenEntity.DEFAULT_MAX_SOURCE,
                "a relay must be able to move a full buffer at once");
        helper.assertTrue(core.getSource() == 0, "a fresh machine must start empty");
        helper.assertTrue(core.canAcceptSource(), "an empty machine must accept source");

        helper.assertTrue(core.addSource(500) == 500, "adding 500 must store 500");
        helper.assertTrue(core.addSource(500) == MagicGenEntity.DEFAULT_MAX_SOURCE,
                "adding past the cap must clamp to 800");
        helper.assertFalse(core.canAcceptSource(), "a full machine must not accept more source");
        helper.assertTrue(core.removeSource(300) == 500, "removing 300 from 800 must leave 500");
        helper.assertTrue(core.removeSource(10_000) == 0, "removing more than stored must clamp to 0");
        helper.assertTrue(core.setSource(-5) == 0, "negative input must clamp to 0");
        helper.succeed();
    }

    /** 魔源数量必须能过存档往返。 */
    @GameTest(template = "empty")
    public static void sourceSurvivesSaveAndLoad(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, BlockRegistries.MAGIC_GEN_MACHINE.get());
        BlockPos absolute = helper.absolutePos(pos);
        MagicGenEntity core = (MagicGenEntity) helper.getLevel().getBlockEntity(absolute);
        helper.assertTrue(core != null, "the machine block entity must exist");
        core.setSource(640);

        CompoundTag saved = core.saveWithoutMetadata();
        MagicGenEntity restored = BlockRegistries.MAGIC_GEN_ENTITY.get()
                .create(absolute, helper.getLevel().getBlockState(absolute));
        helper.assertTrue(restored != null, "the registered block entity type must create a machine");
        restored.setLevel(helper.getLevel());
        restored.load(saved);

        helper.assertTrue(restored.getSource() == 640, "saved source must round-trip, got " + restored.getSource());
        helper.assertTrue(restored.getMaxSource() == MagicGenEntity.DEFAULT_MAX_SOURCE,
                "the 800 cap must round-trip");
        helper.succeed();
    }

    private static Block tank() {
        Block tank = ForgeRegistries.BLOCKS.getValue(FLUID_TANK);
        if (tank == null) {
            throw new IllegalStateException("create:fluid_tank is missing; the ui gate cannot be tested");
        }
        return tank;
    }
}
