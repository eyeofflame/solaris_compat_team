package dev.efm.solaris_core.example;

import dev.efm.solaris_core.api.multiblock.SolarisMasterBlockEntity;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * 绑定在 master 数据上的 FE 存储示例。
 * <p>
 * 不缓存能量值，每次读写都走 {@link SolarisMasterBlockEntity#getData()}，
 * 这样多个 {@code getCapabilityAt} 返回的实例之间不会不同步。
 */
public final class MasterEnergyStorage implements IEnergyStorage {

    public static final String TAG_ENERGY = "Energy";

    private final SolarisMasterBlockEntity master;
    private final int capacity;
    private final int maxReceive;
    private final int maxExtract;

    public MasterEnergyStorage(SolarisMasterBlockEntity master, int capacity, int maxReceive, int maxExtract) {
        this.master = master;
        this.capacity = capacity;
        this.maxReceive = maxReceive;
        this.maxExtract = maxExtract;
    }

    private int stored() {
        return master.getData().getInt(TAG_ENERGY);
    }

    private void store(int energy) {
        master.getData().putInt(TAG_ENERGY, energy);
        // 只标脏存档，不走 sendBlockUpdated：能量每 tick 都在变，不该整块同步
        master.setChanged();
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        if (!canReceive() || toReceive <= 0) {
            return 0;
        }
        int stored = stored();
        int accepted = Math.min(capacity - stored, Math.min(maxReceive, toReceive));
        if (!simulate && accepted > 0) {
            store(stored + accepted);
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        if (!canExtract() || toExtract <= 0) {
            return 0;
        }
        int stored = stored();
        int extracted = Math.min(stored, Math.min(maxExtract, toExtract));
        if (!simulate && extracted > 0) {
            store(stored - extracted);
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return stored();
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }
}
