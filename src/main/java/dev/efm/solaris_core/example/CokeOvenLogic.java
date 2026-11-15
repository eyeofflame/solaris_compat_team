package dev.efm.solaris_core.example;

import dev.efm.solaris_core.api.multiblock.IMultiblockLogic;
import dev.efm.solaris_core.api.multiblock.MultiblockLogics;
import dev.efm.solaris_core.api.multiblock.SolarisMasterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;

/**
 * 给 {@link SMuti#COKE} 挂 FE 存储的示例。
 * <p>
 * 注册后：
 * <ul>
 *     <li>结构里任意一格被别的 mod 的管道/线缆查询 {@code IEnergyStorage} 时，dummy 会转发到 master</li>
 *     <li>master 再调用这里，返回绑在 master 数据上的 {@link MasterEnergyStorage}</li>
 * </ul>
 * 面板里读能量：
 * <pre>{@code
 * int energy = master.getData().getInt(MasterEnergyStorage.TAG_ENERGY);
 * }</pre>
 */
public final class CokeOvenLogic implements IMultiblockLogic {

    public static final int CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 1_000;
    public static final int MAX_EXTRACT = 1_000;

    public static void register() {
        MultiblockLogics.register(SMuti.COKE.getId(), new CokeOvenLogic());
    }

    @Override
    public <T> LazyOptional<T> getCapabilityAt(SolarisMasterBlockEntity master, BlockPos posInMB,
                                               Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return LazyOptional.of(() -> new MasterEnergyStorage(master, CAPACITY, MAX_RECEIVE, MAX_EXTRACT)).cast();
        }
        return LazyOptional.empty();
    }
}
