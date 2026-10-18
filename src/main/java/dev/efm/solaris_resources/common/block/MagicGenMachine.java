package dev.efm.solaris_resources.common.block;

import com.lowdragmc.lowdraglib.gui.factory.BlockEntityUIFactory;
import com.simibubi.create.content.kinetics.base.KineticBlock;
import com.simibubi.create.foundation.block.IBE;
import dev.efm.solaris_resources.common.blockentity.MagicGenEntity;
import dev.efm.solaris_resources.common.registration.BlockRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;

public class MagicGenMachine extends KineticBlock implements IBE<MagicGenEntity> {

    /**
     * 用注册表查而不是 {@code AllBlocks.FLUID_TANK}：Create 把 Registrate 以 jarjar 内嵌，
     * 直接访问 AllBlocks 的字段会让 javac 去解析没有暴露的 BlockEntry 类型。
     */
    private static final ResourceLocation FLUID_TANK_ID = ResourceLocation.fromNamespaceAndPath("create", "fluid_tank");

    public MagicGenMachine(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.AXIS);
    }

    /** 传动杆只允许上下；放置时强制 Y，不接受水平朝向。 */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Y);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state;
    }

    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state;
    }

    @Override
    public Direction.Axis getRotationAxis(BlockState blockState) {
        return blockState.getValue(BlockStateProperties.AXIS);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction side) {
        return side.getAxis() == state.getValue(BlockStateProperties.AXIS);
    }

    /**
     * 竖直放置、且正下方是铜流体储罐时才打开 UI。判定两端同源，避免客户端挥手与服务端不一致。
     */
    public static boolean canOpenUI(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(BlockStateProperties.AXIS) != Direction.Axis.Y) {
            return false;
        }
        return hasFluidTankBelow(level, pos);
    }

    public static boolean hasFluidTankBelow(Level level, BlockPos pos) {
        Block tank = ForgeRegistries.BLOCKS.getValue(FLUID_TANK_ID);
        return tank != null && level.getBlockState(pos.below()).is(tank);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                 BlockHitResult hitResult) {
        if (!canOpenUI(state, level, pos)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MagicGenEntity core && player instanceof ServerPlayer serverPlayer) {
            BlockEntityUIFactory.INSTANCE.openUI(core, serverPlayer);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
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
