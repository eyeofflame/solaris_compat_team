package dev.efm.solaris_compat.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class SolarisConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue FlushTime;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");

        FlushTime = builder.defineInRange("flushTime", 900, 60, 1800);

        builder.pop();

        SPEC = builder.build();
    }
}
