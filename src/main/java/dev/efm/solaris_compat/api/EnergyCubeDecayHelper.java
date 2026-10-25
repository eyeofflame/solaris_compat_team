package dev.efm.solaris_compat.api;

import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.FloatingLong;
import mekanism.common.tier.EnergyCubeTier;
import mekanism.common.tile.TileEntityEnergyCube;

public final class EnergyCubeDecayHelper {

    /**
     * 每多少 tick 泄漏一次。
     */
    public static final int DECAY_INTERVAL = 60;

    /**
     * 0.5% = 1/200。
     */
    private static final long DECAY_DIVISOR = 200L;

    private EnergyCubeDecayHelper() {
    }

    public static void decay(TileEntityEnergyCube cube) {
        if (cube.getTier() == EnergyCubeTier.CREATIVE) {
            return;
        }
        IEnergyContainer container = cube.getEnergyContainer();
        FloatingLong energy = container.getEnergy();
        if (energy.isZero()) {
            return;
        }
        FloatingLong loss = energy.divide(DECAY_DIVISOR);
        if (loss.isZero()) {
            return;
        }

        container.setEnergy(energy.subtract(loss));
    }
}
