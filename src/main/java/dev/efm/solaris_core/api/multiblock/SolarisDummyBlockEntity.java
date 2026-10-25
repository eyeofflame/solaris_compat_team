package dev.efm.solaris_core.api.multiblock;

import dev.efm.solaris_core.common.SolarisRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

/**
 * dummy 方块实体：结构里除 master 之外的每一格。
 * <p>
 * 只存自己在结构里的偏移，能力查询转发给 master；没有 ticker、没有机器状态。
 */
public class SolarisDummyBlockEntity extends BlockEntity {

    private static final String TAG_MASTER_OFFSET = "MasterOffset";

    private BlockPos masterOffset = BlockPos.ZERO;

    public SolarisDummyBlockEntity(BlockPos pos, BlockState state) {
        super(SolarisRegistry.PART_DUMMY_BE.get(), pos, state);
    }

    /** 成型时由 {@link MultiblockHandler} 调用：master 位置 - 本格位置。 */
    public void setMasterOffset(BlockPos masterOffset) {
        this.masterOffset = masterOffset.immutable();
        this.setChanged();
    }

    public BlockPos getMasterOffset() {
        return masterOffset;
    }

    @Nullable
    public SolarisMasterBlockEntity getMaster() {
        if (this.level == null) {
            return null;
        }
        BlockEntity blockEntity = this.level.getBlockEntity(this.worldPosition.subtract(masterOffset));
        return blockEntity instanceof SolarisMasterBlockEntity master ? master : null;
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        SolarisMasterBlockEntity master = getMaster();
        return master != null ? master.getCapabilityAt(masterOffset, cap, side) : super.getCapability(cap, side);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putLong(TAG_MASTER_OFFSET, masterOffset.asLong());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        this.masterOffset = BlockPos.of(tag.getLong(TAG_MASTER_OFFSET));
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        tag.putLong(TAG_MASTER_OFFSET, masterOffset.asLong());
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
