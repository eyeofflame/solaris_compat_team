package dev.efm.solaris_resources.common.block;

import com.simibubi.create.api.stress.BlockStressValues;
import dev.efm.solaris_resources.common.blockentity.MagicGenEntity;
import dev.efm.solaris_resources.common.registration.BlockRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

@GameTestHolder("solaris_resources")
@PrefixGameTestTemplate(false)
public class MagicGenMachineGameTests {

    @GameTest(template = "empty")
    public static void placedMachineIsWired(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        MagicGenMachine block = BlockRegistries.MAGIC_GEN_MACHINE.get();
        helper.assertTrue(ResourceLocation.parse("solaris_resources:test0").equals(ForgeRegistries.BLOCKS.getKey(block)),
                "machine block ID must remain compatible with existing worlds");
        helper.assertTrue(ResourceLocation.parse("solaris_resources:test0").equals(BlockRegistries.MAGIC_GEN_ENTITY.getId()),
                "machine entity ID must remain compatible with existing worlds");
        helper.setBlock(pos, block);
        BlockPos absolute = helper.absolutePos(pos);

        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.assertTrue(state.getBlock() == block, "test0 must be placed");
        helper.assertTrue(state.hasProperty(BlockStateProperties.AXIS)
                && state.getValue(BlockStateProperties.AXIS) == Direction.Axis.Y, "axis property defaults to Y");
        helper.assertTrue(helper.getLevel().getBlockEntity(absolute) instanceof MagicGenEntity,
                "block entity must be created for the placed block");

        MagicGenMachine machine = (MagicGenMachine) block;
        helper.assertTrue(machine.getRotationAxis(state) == Direction.Axis.Y, "rotates around its axis");
        helper.assertTrue(machine.hasShaftTowards(helper.getLevel(), absolute, state, Direction.UP)
                && machine.hasShaftTowards(helper.getLevel(), absolute, state, Direction.DOWN),
                "shaft must connect on both axis ends");
        helper.assertFalse(machine.hasShaftTowards(helper.getLevel(), absolute, state, Direction.NORTH),
                "shaft must not connect on horizontal sides");
        helper.assertTrue(BlockStressValues.getImpact(block) == 8.0, "stress impact of 8 SU must be registered");
        helper.succeed();
    }
}
