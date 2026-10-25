package dev.efm.solaris_core.api.multiblock;

import dev.efm.solaris_core.common.SolarisRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.GeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * master 方块实体：整台机器的状态都在这里（图纸 id、旋转、尺寸、所有格子的相对偏移、机器自定义数据）。
 * <p>
 * 只有 {@code SLAVE=false} 的那一格会创建它；其余格是 {@link SolarisDummyBlockEntity}，
 * 只负责把能力查询转发过来。机器行为通过 {@link IMultiblockLogic} 按图纸 id 注册。
 * <p>
 * 部件偏移列表会同步给客户端，用于 BER 的兜底渲染（没注册整机模型时逐格画方块）。
 */
public class SolarisMasterBlockEntity extends BlockEntity implements GeoAnimatable {

    private static final String TAG_MULTIBLOCK = "Multiblock";
    private static final String TAG_ROTATION = "Rotation";
    private static final String TAG_DATA = "Data";
    private static final String TAG_SIZE = "Size";
    private static final String TAG_PARTS = "Parts";

    @Nullable
    private ResourceLocation multiblockId;
    private Rotation rotation = Rotation.NONE;
    private Vec3i size = new Vec3i(1, 1, 1);
    private final List<BlockPos> parts = new ArrayList<>();
    private final CompoundTag data = new CompoundTag();
    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);
    private boolean disassembling;

    public SolarisMasterBlockEntity(BlockPos pos, BlockState state) {
        super(SolarisRegistry.PART_MASTER_BE.get(), pos, state);
    }

    /** 成型时由 {@link MultiblockHandler} 调用。 */
    public void setup(ResourceLocation multiblockId, Rotation rotation, Vec3i size, List<BlockPos> parts) {
        this.multiblockId = multiblockId;
        this.rotation = rotation;
        this.size = size;
        this.parts.clear();
        this.parts.addAll(parts);
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    @Nullable
    public ResourceLocation getMultiblockId() {
        return multiblockId;
    }

    @Nullable
    public Multiblock getMultiblock() {
        return multiblockId == null ? null : MultiblockRegistry.get(multiblockId);
    }

    public Rotation getRotation() {
        return rotation;
    }

    /** 结构尺寸（模板 size），客户端渲染包围盒用。 */
    public Vec3i getSize() {
        return size;
    }

    /** 结构里每一格相对 master 的偏移（含 {@link BlockPos#ZERO}），客户端兜底渲染用。 */
    public List<BlockPos> getParts() {
        return parts;
    }

    /** 机器自己的持久化数据（库存、进度、能量……），随方块实体存档。 */
    public CompoundTag getData() {
        return data;
    }

    public void markDirty() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void serverTick() {
        if (this.level instanceof ServerLevel serverLevel && multiblockId != null) {
            IMultiblockLogic logic = MultiblockLogics.get(multiblockId);
            if (logic != null) {
                logic.tick(serverLevel, this);
            }
        }
    }

    // ---- GeckoLib ----

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        IMultiblockLogic logic = multiblockId == null ? null : MultiblockLogics.get(multiblockId);
        if (logic != null) {
            logic.registerControllers(this, controllers);
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return animatableCache;
    }

    @Override
    public double getTick(Object animatable) {
        return this.level == null ? 0.0D : this.level.getGameTime();
    }

    /** 按"结构内相对位置 + 面"提供能力；dummy 会把查询转发到这里。 */
    public <T> LazyOptional<T> getCapabilityAt(BlockPos posInMB, Capability<T> cap, @Nullable Direction side) {
        IMultiblockLogic logic = MultiblockLogics.get(multiblockId);
        if (logic != null) {
            LazyOptional<T> provided = logic.getCapabilityAt(this, posInMB, cap, side);
            if (provided.isPresent()) {
                return provided;
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
        return getCapabilityAt(BlockPos.ZERO, cap, side);
    }

    /**
     * 整机渲染包围盒：按同步过来的部件偏移 + 旋转精确计算。
     * （不能用 master 的 origin+size，因为旋转不是绕 origin 的。）
     */
    @Override
    public AABB getRenderBoundingBox() {
        if (parts.isEmpty()) {
            return new AABB(worldPosition);
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos offset : parts) {
            BlockPos rotated = offset.rotate(rotation);
            minX = Math.min(minX, rotated.getX());
            minY = Math.min(minY, rotated.getY());
            minZ = Math.min(minZ, rotated.getZ());
            maxX = Math.max(maxX, rotated.getX() + 1);
            maxY = Math.max(maxY, rotated.getY() + 1);
            maxZ = Math.max(maxZ, rotated.getZ() + 1);
        }
        return new AABB(
                worldPosition.getX() + minX, worldPosition.getY() + minY, worldPosition.getZ() + minZ,
                worldPosition.getX() + maxX, worldPosition.getY() + maxY, worldPosition.getZ() + maxZ
        );
    }

    boolean isDisassembling() {
        return disassembling;
    }

    void setDisassembling(boolean disassembling) {
        this.disassembling = disassembling;
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeSync(tag);
        tag.put(TAG_DATA, data.copy());
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        readSync(tag);
        CompoundTag loaded = tag.getCompound(TAG_DATA);
        this.data.getAllKeys().clear();
        this.data.merge(loaded);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        writeSync(tag);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void writeSync(CompoundTag tag) {
        if (multiblockId != null) {
            tag.putString(TAG_MULTIBLOCK, multiblockId.toString());
        }
        tag.putString(TAG_ROTATION, rotation.name());
        tag.putIntArray(TAG_SIZE, new int[]{size.getX(), size.getY(), size.getZ()});
        ListTag partList = new ListTag();
        for (BlockPos part : parts) {
            partList.add(LongTag.valueOf(part.asLong()));
        }
        tag.put(TAG_PARTS, partList);
    }

    private void readSync(CompoundTag tag) {
        this.multiblockId = ResourceLocation.tryParse(tag.getString(TAG_MULTIBLOCK));
        try {
            this.rotation = Rotation.valueOf(tag.getString(TAG_ROTATION));
        } catch (IllegalArgumentException ignored) {
            this.rotation = Rotation.NONE;
        }
        int[] sizeArray = tag.getIntArray(TAG_SIZE);
        if (sizeArray.length == 3) {
            this.size = new Vec3i(sizeArray[0], sizeArray[1], sizeArray[2]);
        }
        this.parts.clear();
        ListTag partList = tag.getList(TAG_PARTS, Tag.TAG_LONG);
        for (int i = 0; i < partList.size(); i++) {
            this.parts.add(BlockPos.of(((LongTag) partList.get(i)).getAsLong()));
        }
    }
}
