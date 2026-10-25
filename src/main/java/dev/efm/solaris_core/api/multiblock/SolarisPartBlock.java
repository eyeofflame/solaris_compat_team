package dev.efm.solaris_core.api.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.storage.loot.LootParams;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 多方块部件方块：成型后整座结构每一格都是它。
 * <ul>
 *     <li>{@code slave=false} 的那一格创建 {@link SolarisMasterBlockEntity}</li>
 *     <li>其余格创建 {@link SolarisDummyBlockEntity}</li>
 * </ul>
 * 破坏任意一格会触发整座拆解（{@link MultiblockHandler#disassemble}）。
 * <p>
 * 方块本身不渲染（{@code INVISIBLE}），整机由 master 的 GeckoLib 渲染器绘制。
 * 同时它不挡光，否则被部件方块包住的 master 光照为 0，整机在游戏里会发暗。
 */
public class SolarisPartBlock extends BaseEntityBlock {

    public static final BooleanProperty SLAVE = BooleanProperty.create("slave");

    public SolarisPartBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SLAVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SLAVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(SLAVE)
                ? new SolarisDummyBlockEntity(pos, state)
                : new SolarisMasterBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // 整机由 master 的 BER 渲染（见 client/multiblock），方块本身不画模型
        return RenderShape.INVISIBLE;
    }

    /**
     * 不挡光：部件方块占满整座结构，如果不透明，master 位置的光照会是 0，
     * BER 拿到的 packedLight 就是黑的，整机看起来发暗。
     */
    @Override
    public int getLightBlock(BlockState state, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || state.getValue(SLAVE)) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> {
            if (blockEntity instanceof SolarisMasterBlockEntity master) {
                master.serverTick();
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            SolarisMasterBlockEntity master = getMaster(level, pos);
            if (master != null) {
                IMultiblockLogic logic = MultiblockLogics.get(master.getMultiblockId());
                if (logic != null) {
                    logic.onInteract(serverPlayer, master);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            SolarisMasterBlockEntity master = resolveMaster(level, pos);
            if (master != null) {
                MultiblockHandler.disassemble(serverLevel, master, pos);
            }
        }
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        // 机器本体不直接掉落；材料由拆解还原，机器内容由 IMultiblockLogic#onBroken 处理
        return List.of();
    }

    @Nullable
    public static SolarisMasterBlockEntity getMaster(Level level, BlockPos pos) {
        return resolveMaster(level, pos);
    }

    @Nullable
    private static SolarisMasterBlockEntity resolveMaster(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SolarisMasterBlockEntity master) {
            return master;
        }
        if (blockEntity instanceof SolarisDummyBlockEntity dummy) {
            return dummy.getMaster();
        }
        return null;
    }
}
