package dev.efm.solaris_resources.common.block;

import com.simibubi.create.api.stress.BlockStressValues;
import dev.efm.solaris_resources.common.blockentity.MagicGenEntity;
import dev.efm.solaris_resources.common.registration.BlockRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

@GameTestHolder("solaris_resources")
@PrefixGameTestTemplate(false)
public class MagicGenMachineGameTests {

    @GameTest(template = "empty")
    public static void placedMachineIsWired(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        MagicGenMachine block = BlockRegistries.MAGIC_GEN_MACHINE.get();
        helper.assertTrue(ResourceLocation.parse("solaris_resources:unstable_machanical_core").equals(ForgeRegistries.BLOCKS.getKey(block)),
                "machine block ID must match its model resources");
        helper.assertTrue(ResourceLocation.parse("solaris_resources:unstable_machanical_core").equals(BlockRegistries.MAGIC_GEN_ENTITY.getId()),
                "machine entity ID must match the current registration");
        helper.setBlock(pos, block);
        BlockPos absolute = helper.absolutePos(pos);

        BlockState state = helper.getLevel().getBlockState(absolute);
        helper.assertTrue(state.getBlock() == block, "unstable mechanical core must be placed");
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
        helper.assertTrue(((MagicGenEntity) helper.getLevel().getBlockEntity(absolute)).calculateStressApplied() == 8f,
                "the entity must report the same registered impact, not a second hardcoded value");

        helper.assertTrue(state.requiresCorrectToolForDrops(), "the machine must require the correct tool for drops");
        helper.assertTrue(state.is(BlockTags.MINEABLE_WITH_PICKAXE), "pickaxe must be the correct tool for the machine");
        helper.assertTrue(state.is(BlockTags.NEEDS_STONE_TOOL), "harvest level must be stone or better");
        helper.assertTrue(new ItemStack(Items.STONE_PICKAXE).isCorrectToolForDrops(state),
                "stone pickaxe must harvest the machine");
        helper.assertTrue(new ItemStack(Items.IRON_PICKAXE).isCorrectToolForDrops(state),
                "iron pickaxe must harvest the machine");
        helper.assertTrue(new ItemStack(Items.NETHERITE_PICKAXE).isCorrectToolForDrops(state),
                "netherite pickaxe must harvest the machine");
        helper.assertFalse(new ItemStack(Items.WOODEN_PICKAXE).isCorrectToolForDrops(state),
                "wooden pickaxe must not harvest the machine");
        helper.assertFalse(new ItemStack(Items.GOLDEN_PICKAXE).isCorrectToolForDrops(state),
                "golden pickaxe must not harvest the machine");
        helper.assertFalse(new ItemStack(Items.IRON_SHOVEL).isCorrectToolForDrops(state),
                "a shovel must not harvest the machine");
        helper.assertFalse(ItemStack.EMPTY.isCorrectToolForDrops(state), "bare hands must not harvest the machine");
        helper.assertTrue(Blocks.IRON_BLOCK.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)
                        && Blocks.IRON_BLOCK.defaultBlockState().is(BlockTags.NEEDS_STONE_TOOL),
                "our tag files must merge into the vanilla tags, never replace them");

        List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), absolute,
                helper.getLevel().getBlockEntity(absolute));
        helper.assertTrue(drops.stream().anyMatch(stack -> stack.is(BlockRegistries.TEST2.get())),
                "breaking the machine must drop its block item, got " + drops);
        helper.succeed();
    }
}
