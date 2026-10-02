package dev.efm.solaris_progress;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 原点平坦平原 worldgen 的配置。加载后把值快照到静态字段，供 worldgen 热路径读取。
 */
public final class SolaConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.IntValue FLAT_Y;
    private static final ForgeConfigSpec.IntValue HALF_EXTENT;
    private static final ForgeConfigSpec.IntValue BLEND_WIDTH;
    private static final ForgeConfigSpec.IntValue CENTER_X;
    private static final ForgeConfigSpec.IntValue CENTER_Z;
    private static final ForgeConfigSpec.BooleanValue PLACE_INIT;
    private static final ForgeConfigSpec.IntValue INIT_Y_OFFSET;
    private static final ForgeConfigSpec.IntValue STRUCTURE_BLOCK_MARGIN;

    public static volatile boolean enabled = true;
    public static volatile double flatY = 64.0;
    public static volatile double halfExtent = 128.0;
    public static volatile double blendWidth = 32.0;
    public static volatile double centerX = 0.0;
    public static volatile double centerZ = 0.0;
    public static volatile boolean placeInit = true;
    public static volatile int initYOffset = -1;
    public static volatile int structureBlockMargin = 128;

    /** Worldgen 参数在首次加载后冻结：保证地形包裹与群系/出生点读到同一份快照，修改需重启。 */
    private static volatile boolean frozen = false;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("spawn_flatland");
        ENABLED = b.comment("启用原点平坦平原 worldgen（仅对新存档/新区块生效）")
                .define("enabled", true);
        FLAT_Y = b.comment("平定后地表固体顶面 Y，必须 > 海平面 63，否则会出现水面")
                .defineInRange("flatY", 64, 64, 300);
        HALF_EXTENT = b.comment("正方形平坦核心半边长（256 表示 256x256）")
                .defineInRange("halfExtent", 128, 16, 1024);
        BLEND_WIDTH = b.comment("核心外向原版地形过渡的带宽")
                .defineInRange("blendWidth", 32, 0, 512);
        CENTER_X = b.comment("区域中心 X")
                .defineInRange("centerX", 0, -30_000_000, 30_000_000);
        CENTER_Z = b.comment("区域中心 Z")
                .defineInRange("centerZ", 0, -30_000_000, 30_000_000);
        PLACE_INIT = b.comment("新存档时把结构 solaris_progress:init 放到平坦核心正中")
                .define("placeInit", true);
        INIT_Y_OFFSET = b.comment("结构最底层(相对 Y=0) 的世界 Y = flatY + 该偏移（-1 → 63）")
                .defineInRange("initYOffset", -1, -64, 128);
        STRUCTURE_BLOCK_MARGIN = b.comment("结构生成屏蔽额外缓冲，避免结构从平坦区外侧伸入")
                .defineInRange("structureBlockMargin", 128, 0, 256);
        b.pop();
        SPEC = b.build();
    }

    private SolaConfig() {}

    public static void refresh() {
        if (frozen) return;
        enabled = ENABLED.get();
        flatY = FLAT_Y.get();
        halfExtent = HALF_EXTENT.get();
        blendWidth = BLEND_WIDTH.get();
        centerX = CENTER_X.get();
        centerZ = CENTER_Z.get();
        placeInit = PLACE_INIT.get();
        initYOffset = INIT_Y_OFFSET.get();
        structureBlockMargin = STRUCTURE_BLOCK_MARGIN.get();
        frozen = true;
    }

    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            refresh();
        }
    }
}
