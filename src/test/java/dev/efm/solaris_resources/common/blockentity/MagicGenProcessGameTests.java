package dev.efm.solaris_resources.common.blockentity;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.ProgressWidget;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.registration.BlockRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;


@GameTestHolder(SolarisResources.MODID)
@PrefixGameTestTemplate(false)
public class MagicGenProcessGameTests {

    private static final ResourceLocation FLUID_TANK = ResourceLocation.fromNamespaceAndPath("create", "fluid_tank");

    /** 转速 → 时长曲线：32 rpm = 200 tick，256 rpm = 20 tick，中间线性，两端截断。 */
    @GameTest(template = "empty")
    public static void ticksPerProcessMatchesCalibration(GameTestHelper helper) {
        helper.assertTrue(MagicGenEntity.ticksPerProcess(0) == 0, "0 rpm must not process");
        helper.assertTrue(MagicGenEntity.ticksPerProcess(31.9F) == 0, "below 32 rpm must not process");
        helper.assertTrue(MagicGenEntity.ticksPerProcess(MagicGenEntity.MIN_RPM) == 200, "32 rpm must take 200 ticks");
        helper.assertTrue(MagicGenEntity.ticksPerProcess(MagicGenEntity.FAST_RPM) == 20, "256 rpm must take 20 ticks");
        helper.assertTrue(MagicGenEntity.ticksPerProcess(512F) == 20, "faster than 256 rpm must stay at 20 ticks");
        helper.assertTrue(MagicGenEntity.ticksPerProcess(144F) == 110,
                "midpoint must interpolate linearly, got " + MagicGenEntity.ticksPerProcess(144F));
        helper.succeed();
    }

    /** 完整跑一次：20 tick 内吸走 1B 水 + 15 魔源。 */
    @GameTest(template = "empty")
    public static void processConsumesWaterAndSource(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MagicGenEntity core = placeMachineOverTank(helper, pos);
        IFluidHandler tank = tankBelow(helper, pos);
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE) == 4000,
                "the tank must accept 4B of water");
        core.setSource(100);

        boolean completed = false;
        for (int i = 0; i < MagicGenEntity.TICKS_AT_FAST_RPM; i++) {
            completed = core.tickProcessing(MagicGenEntity.FAST_RPM);
        }

        helper.assertTrue(completed, "256 rpm must finish a process in exactly 20 ticks");
        helper.assertTrue(core.getCompletedProcesses() == 1, "one process must be recorded");
        helper.assertTrue(core.getSource() == 100 - MagicGenEntity.SOURCE_PER_PROCESS,
                "15 source must be consumed, got " + core.getSource());
        helper.assertTrue(core.getProgress() == 0.0F, "progress must reset after completion");
        helper.assertTrue(tank.getFluidInTank(0).getAmount() == 3000,
                "1B water must be consumed, got " + tank.getFluidInTank(0).getAmount());
        helper.succeed();
    }

    /** 液体类型必须真的是水；岩浆不算。 */
    @GameTest(template = "empty")
    public static void processRejectsWrongFluid(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MagicGenEntity core = placeMachineOverTank(helper, pos);
        IFluidHandler tank = tankBelow(helper, pos);
        tank.fill(new FluidStack(Fluids.LAVA, 4000), IFluidHandler.FluidAction.EXECUTE);
        core.setSource(100);

        for (int i = 0; i < 40; i++) {
            helper.assertFalse(core.tickProcessing(MagicGenEntity.FAST_RPM), "lava must never be treated as water");
        }
        helper.assertTrue(core.getCompletedProcesses() == 0, "nothing may be processed");
        helper.assertTrue(core.getSource() == 100, "no source may be consumed");
        helper.assertTrue(tank.getFluidInTank(0).getAmount() == 4000, "the lava must stay untouched");
        helper.succeed();
    }

    /** 没有储罐 / 没有水时停机，进度与资源都不动。 */
    @GameTest(template = "empty")
    public static void processStallsWithoutWater(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, BlockRegistries.MAGIC_GEN_MACHINE.get());
        MagicGenEntity core = (MagicGenEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(core != null, "the machine block entity must exist");
        core.setSource(100);

        for (int i = 0; i < 40; i++) {
            helper.assertFalse(core.tickProcessing(MagicGenEntity.FAST_RPM), "no tank below must not process");
        }
        helper.assertTrue(core.getCompletedProcesses() == 0, "nothing may be processed");
        helper.assertTrue(core.getSource() == 100, "no source may be consumed");
        helper.succeed();
    }

    /** 魔源不够 15 时同样停机。 */
    @GameTest(template = "empty")
    public static void processStallsWithoutSource(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MagicGenEntity core = placeMachineOverTank(helper, pos);
        IFluidHandler tank = tankBelow(helper, pos);
        tank.fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        core.setSource(MagicGenEntity.SOURCE_PER_PROCESS - 1);

        for (int i = 0; i < 40; i++) {
            helper.assertFalse(core.tickProcessing(MagicGenEntity.FAST_RPM), "not enough source must not process");
        }
        helper.assertTrue(core.getCompletedProcesses() == 0, "nothing may be processed");
        helper.assertTrue(tank.getFluidInTank(0).getAmount() == 4000, "the water must stay untouched");
        helper.succeed();
    }

    /** UI 里的 sola_progress 必须是进度条，并且取值器读的是机器进度。 */
    @GameTest(template = "empty")
    public static void progressWidgetFollowsMachineProgress(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        MagicGenEntity core = placeMachineOverTank(helper, pos);
        IFluidHandler tank = tankBelow(helper, pos);
        tank.fill(new FluidStack(Fluids.WATER, 4000), IFluidHandler.FluidAction.EXECUTE);
        core.setSource(100);

        ModularUI ui = core.createUI(helper.makeMockPlayer());
        var widget = ui.mainGroup.getFirstWidgetById(MagicGenEntity.PROGRESS_ID);
        helper.assertTrue(widget instanceof ProgressWidget,
                "magic_core_i.ui must expose " + MagicGenEntity.PROGRESS_ID + " as a progress widget, got " + widget);

        ProgressWidget progress = (ProgressWidget) widget;
        helper.assertTrue(progress.progressSupplier != null, "the progress widget must be bound to the machine");
        helper.assertTrue(progress.progressSupplier.getAsDouble() == 0.0D, "a fresh machine must show 0 progress");

        // 32 rpm 时一次处理 200 tick，走 20 tick 就是 10% 进度。
        for (int i = 0; i < 20; i++) {
            core.tickProcessing(MagicGenEntity.MIN_RPM);
        }
        helper.assertTrue(Math.abs(progress.progressSupplier.getAsDouble() - 0.1D) < 1e-6,
                "the bar must follow the machine progress, got " + progress.progressSupplier.getAsDouble());
        helper.succeed();
    }

    /** 工程师护目镜：转速不够时补一行"应力不足"警告。 */
    @GameTest(template = "empty")
    public static void gogglesWarnWhenSpeedTooLow(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, BlockRegistries.MAGIC_GEN_MACHINE.get());
        MagicGenEntity core = (MagicGenEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        helper.assertTrue(core != null, "the machine block entity must exist");

        core.setSpeed(0.0F);
        helper.assertTrue(core.stressWarning().isPresent(), "a stopped machine must warn about insufficient stress");
        helper.assertTrue(hasStressWarning(core.stressWarning().orElseThrow()),
                "the warning must use the translatable goggle key");

        core.setSpeed(MagicGenEntity.MIN_RPM);
        helper.assertTrue(core.stressWarning().isEmpty(), "32 rpm must satisfy the requirement");

        core.setSpeed(-MagicGenEntity.FAST_RPM);
        helper.assertTrue(core.stressWarning().isEmpty(), "reversed rotation at 256 rpm is fine too");

        core.setSpeed(MagicGenEntity.MIN_RPM - 0.1F);
        helper.assertTrue(core.stressWarning().isPresent(), "just below 32 rpm must still warn");
        helper.succeed();
    }

    private static boolean hasStressWarning(Component component) {
        return component.getContents() instanceof TranslatableContents translatable
                && translatable.getKey().equals("goggles.solaris_resources.stress_too_low");
    }

    private static MagicGenEntity placeMachineOverTank(GameTestHelper helper, BlockPos pos) {        Block tank = ForgeRegistries.BLOCKS.getValue(FLUID_TANK);
        if (tank == null) {
            throw new IllegalStateException("create:fluid_tank is missing");
        }
        helper.setBlock(pos.below(), tank);
        helper.setBlock(pos, BlockRegistries.MAGIC_GEN_MACHINE.get());
        MagicGenEntity core = (MagicGenEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (core == null) {
            throw new IllegalStateException("the machine block entity must exist");
        }
        return core;
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
