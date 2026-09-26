package dev.efm.mekanism_agriculture.common.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class MekAgriConfig {
    public static final ForgeConfigSpec SPEC;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        SPEC = builder.build();
    }
}
