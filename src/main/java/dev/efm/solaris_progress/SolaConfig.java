package dev.efm.solaris_progress;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.event.config.ModConfigEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
    private static final ForgeConfigSpec.BooleanValue BLOCK_END_PORTAL;
    private static final ForgeConfigSpec.BooleanValue PROTECT_SETTLEMENT;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ALLOWED_INTERACTION_ENTITIES;
    private static final ForgeConfigSpec.BooleanValue BLOCK_HOSTILE_MOBS;
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLOCKED_ADDITIONAL_ENTITIES;

    public static volatile boolean enabled = true;
    public static volatile double flatY = 64.0;
    public static volatile double halfExtent = 128.0;
    public static volatile double blendWidth = 32.0;
    public static volatile double centerX = 0.0;
    public static volatile double centerZ = 0.0;
    public static volatile boolean placeInit = true;
    public static volatile int initYOffset = -1;
    public static volatile int structureBlockMargin = 128;
    public static volatile boolean blockEndPortalActivation = true;
    public static volatile boolean protectSettlement = true;
    /** 领地内允许交互的实体类型 ID（小写），由 {@link #ALLOWED_INTERACTION_ENTITIES} 归一化而来。 */
    public static volatile Set<String> allowedInteractionEntities = Set.of();
    /** 聚落领地内是否完全屏蔽敌对生物（{@code Enemy}）。 */
    public static volatile boolean blockHostileMobs = true;
    /** 除 Enemy 外，在领地内也按敌对生物屏蔽的实体类型 ID（小写）。 */
    public static volatile Set<String> blockedAdditionalEntities = Set.of();

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

        b.push("progression");
        BLOCK_END_PORTAL = b.comment("阻止玩家在主世界激活末地传送门（新玩家的末地开局流程不由此开关控制）")
                .define("blockEndPortalActivation", true);
        b.pop();

        b.push("settlement");
        PROTECT_SETTLEMENT = b.comment("""
                启动时自动把主世界 spawn_flatland 区域（中心 centerX/centerZ，边长 2*halfExtent）
                圈定为 FTB Teams 服务器团队 efm_server 的领地：非成员禁止破坏/交互方块、使用物品、伤害无害生物。
                PvP 不受影响；领地范围跟随 spawn_flatland 配置，修改后需重启，旧领地不会自动迁移。""")
                .define("protectSettlement", true);
        ALLOWED_INTERACTION_ENTITIES = b.comment("""
                领地内允许玩家右键交互的实体类型 ID 白名单（可多个），例如：
                ["minecraft:villager", "minecraft:horse"]
                白名单内的实体在聚落领地内可正常交互（交易/骑乘/喂食/拴绳等），其余实体交互仍被禁止。""")
                .defineListAllowEmpty("allowedInteractionEntities", List.of(), o -> o instanceof String);
        BLOCK_HOSTILE_MOBS = b.comment("聚落领地内完全屏蔽敌对生物（Enemy）：阻止生成、阻止进入、清除已进入者")
                .define("blockHostileMobs", true);
        BLOCKED_ADDITIONAL_ENTITIES = b.comment("""
                除 Enemy 外，在领地内也按敌对生物屏蔽的实体类型 ID 白名单（可多个），例如：
                ["minecraft:wolf", "minecraft:bee", "minecraft:polar_bear", "minecraft:panda",
                 "minecraft:iron_golem", "minecraft:goat", "minecraft:llama", "minecraft:trader_llama",
                 "minecraft:pufferfish"]
                留空则只屏蔽 Enemy。""")
                .defineListAllowEmpty("blockedAdditionalEntities", List.of(), o -> o instanceof String);
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
        blockEndPortalActivation = BLOCK_END_PORTAL.get();
        protectSettlement = PROTECT_SETTLEMENT.get();
        allowedInteractionEntities = normalizeIds(ALLOWED_INTERACTION_ENTITIES.get());
        blockHostileMobs = BLOCK_HOSTILE_MOBS.get();
        blockedAdditionalEntities = normalizeIds(BLOCKED_ADDITIONAL_ENTITIES.get());
        frozen = true;
    }

    /** 归一化实体 ID：去空白、转小写、剔除空项。匹配时用注册表键的字符串形式比对。 */
    private static Set<String> normalizeIds(List<? extends String> raw) {
        Set<String> ids = new HashSet<>();
        for (String s : raw) {
            if (s == null) continue;
            String id = s.trim().toLowerCase(Locale.ROOT);
            if (!id.isEmpty()) {
                ids.add(id);
            }
        }
        return Set.copyOf(ids);
    }

    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) {
            refresh();
        }
    }
}
