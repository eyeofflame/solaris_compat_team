package dev.efm.solaris_compat.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class SolarisConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.IntValue FlushTime;
    public static final ForgeConfigSpec.BooleanValue CoinPouchAutoCollect;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("general");

        FlushTime = builder.defineInRange("flushTime", 900, 60, 1800);

        builder.pop();

        builder.push("coin_pouch");
        CoinPouchAutoCollect = builder.comment("装备着硬币袋时，自动把背包里的硬币与硬币堆收进袋里")
                .define("autoCollect", true);
        builder.pop();

        SPEC = builder.build();
    }
}
