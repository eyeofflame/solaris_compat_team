package dev.efm.solaris_core.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class SolarisConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue ENABLE_EAT;
    public static final ForgeConfigSpec.BooleanValue ENABLE_RESPROC;
    public static final ForgeConfigSpec.IntValue RESPROC_TIME;
    public static final ForgeConfigSpec.IntValue CLEAN_TIME;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("general");

        ENABLE_EAT = builder.define("enableEat", true);
        ENABLE_RESPROC = builder.define("enableRESPROC", true);
        RESPROC_TIME = builder.defineInRange("resproc_time", 10, 5, 60);
        CLEAN_TIME = builder.defineInRange("clean_time", 5 * 60, 60, 30 * 60);

        builder.pop();

        SPEC = builder.build();
    }
}
