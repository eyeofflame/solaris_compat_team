package dev.efm.mekanism_agriculture.common.registration;

import java.util.function.UnaryOperator;
import mekanism.common.block.prefab.BlockTile;
import mekanism.common.content.blocktype.BlockTypeTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

/**
 * 注魔机方块。
 *
 * <p>唯一的特殊之处是<b>形状延伸到上方一格</b>:模型总高 30/16 ≈ 1.9 个方块,原版默认的
 * 1×1×1 碰撞箱只盖到柱身中段,上段(柱头、八颗小水晶、中央水晶)全是空的,玩家能直接穿过去。
 * 这里把碰撞与选择形状都改成 1×2×1(即 16×32×16),让整台机器都实心。
 *
 * <p>配合 {@code noOcclusion()}:方块不遮挡邻居,所以形状取成两格高也不会触发原版的相邻面剔除
 * (那正是之前"相邻方块显示为虚空"的根因)。
 *
 * <p>注意形状只影响碰撞、选取和射线检测;放置朝向由原版 {@code BlockPlaceContext} 决定,
 * 不受这里的形状影响。
 */
public class BlockMekInfusioner extends BlockTile<TileEntityMekInfusioner, BlockTypeTile<TileEntityMekInfusioner>> {

    /** 单位是方块:1.0 = 16 像素。这里 0→2 即两格高。 */
    private static final VoxelShape SHAPE = Shapes.box(0, 0, 0, 1, 2, 1);

    public BlockMekInfusioner(BlockTypeTile<TileEntityMekInfusioner> type, UnaryOperator<BlockBehaviour.Properties> propertiesModifier) {
        super(type, propertiesModifier);
    }

    @NotNull
    @Override
    public VoxelShape getShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }

    @NotNull
    @Override
    public VoxelShape getCollisionShape(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        return SHAPE;
    }
}
