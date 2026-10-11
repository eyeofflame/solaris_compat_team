package dev.efm.solaris_resources.common.block;

import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.efm.solaris_resources.common.blockentity.MagicGenEntity;
import dev.efm.solaris_resources.common.registration.BlockRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class MagicGenMachine extends KineticBlock implements IBE<MagicGenEntity> {
    public MagicGenMachine(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.AXIS);
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState blockState) {
        return blockState.getValue(BlockStateProperties.AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction side) {
        return side.getAxis() == state.getValue(BlockStateProperties.AXIS);
    }

    @Override
    public Class<MagicGenEntity> getBlockEntityClass() {
        return MagicGenEntity.class;
    }

    @Override
    public BlockEntityType<? extends MagicGenEntity> getBlockEntityType() {
        return BlockRegistries.MAGIC_GEN_ENTITY.get();
    }
}
