# 原点平坦平原 worldgen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新建存档时，把以原点 `(0,0)` 为中心的 256×256 正方形区域生成为平坦的 `plains` 平原，并把出生点钉在原点，为 `init.nbt` 提供地基。

**Architecture:** `ChunkMap` mixin 在主世界把 `RandomState.router` 的 `finalDensity` / `initialDensityWithoutJaggedness` 包成 `FlatBlendDensityFunction`（区域内按切比雪夫距离与平地密度混合）；`MultiNoiseBiomeSource` mixin 在区域内强制 `plains`；`LevelEvent.CreateSpawnPosition` 强制出生点。

**Tech Stack:** Java 17, Minecraft 1.20.1, Forge 47.4.10, SpongePowered Mixin, JUnit 5。

**Spec:** `docs/superpowers/specs/2026-10-02-solaris-spawn-flat-plains-design.md`

## Global Constraints

- MC 1.20.1 / Forge 47.4.10 / Java 17 / Parchment 2023.09.03-1.20.1。
- mod id `solaris_progress`；mixin 包 `dev.efm.solaris_progress.mixins`。
- `flatY` 默认 64，且必须 > 海平面 63（否则产生水面）。
- 区域是 256×256 **正方形**：判定与混合用切比雪夫距离 `d = max(|dx|, |dz|)`。
- 只影响主世界（`Level.OVERWORLD`），且只对新生成区块生效（需新存档）。
- 新增 mixin 必须登记进 `src/main/resources/solaris_progress.mixins.json` 的 `mixins` 数组。
- 保留：surface 草地/泥土、carver 经典洞穴、OreVeinifier 矿脉、biome feature 植被。
- 项目无 `gradlew`，用全局 `gradle`（当前 8.14.5）。

## Review Focus

以下是最容易被忽略、必须在对应任务里钉住的行为（正文任务含对应测试或验证步骤）：

1. **区域外零改动**：`d >= halfExtent+blendWidth` 的列必须与 vanilla 逐位一致（Task 1 `outside` 测试 + Task 3 `outside_returns_vanilla_untouched`）。
2. **负坐标 / 跨区块绝对坐标**：必须用绝对方块坐标，不能用 chunk-local（Task 1 负坐标对称测试）。
3. **`mapAll` 递归**：必须把 visitor 应用到内部原版密度，否则内部密度失去插值/缓存（Task 3 `mapAll_recurses_into_vanilla`）。
4. **`enabled=false` 完全不改世界**（Task 4 早退 + Task 7 验证）。
5. **`flatY<=63` 产生水面 / `flatY` 越界**（Task 2 夹取 + Task 7 验证）。

---

### Task 1: 测试基础设施 + `FlatBlendMath`（纯数学，TDD）

**Files:**
- Modify: `build.gradle`（`repositories`、`dependencies`、`test`）
- Create: `src/main/java/dev/efm/solaris_progress/worldgen/FlatBlendMath.java`
- Test: `src/test/java/dev/efm/solaris_progress/worldgen/FlatBlendMathTest.java`

**Interfaces:**
- Produces: `FlatBlendMath.smoothstep(double)`、`chebyshev(double,double)`、`blendFactor(double,double,double)`、`outside(double,double,double)`、`flatDensity(double,double)`、`blend(double,double,double)`、`edgeDither(int,int)` —— 全部 `public static`，无 MC 依赖。

- [ ] **Step 1: 在 `build.gradle` 加入 JUnit 与测试任务**

在 `repositories { ... }` 的 `flatDir` 之前加 `mavenCentral()`；在 `dependencies { ... }` 末尾加：

```groovy
    testImplementation 'org.junit.jupiter:junit-jupiter:5.10.2'
```

在文件末尾加：

```groovy
tasks.named('test').configure {
    useJUnitPlatform()
    testLogging { events 'passed', 'failed', 'skipped' }
}
```

- [ ] **Step 2: 写失败的测试**

`FlatBlendMathTest.java`：

```java
package dev.efm.solaris_progress.worldgen;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlatBlendMathTest {
    @Test void smoothstep_clamps_and_is_monotonic() {
        assertEquals(0.0, FlatBlendMath.smoothstep(0.0), 1e-9);
        assertEquals(1.0, FlatBlendMath.smoothstep(1.0), 1e-9);
        assertEquals(0.5, FlatBlendMath.smoothstep(0.5), 1e-9);
        assertTrue(FlatBlendMath.smoothstep(0.2) < FlatBlendMath.smoothstep(0.8));
    }

    @Test void chebyshev_uses_abs_max() {
        assertEquals(5.0, FlatBlendMath.chebyshev(-5.0, 3.0), 1e-9);
        assertEquals(3.0, FlatBlendMath.chebyshev(2.0, -3.0), 1e-9);
    }

    @Test void blendFactor_zero_inside_one_outside() {
        assertEquals(0.0, FlatBlendMath.blendFactor(100.0, 128.0, 32.0), 1e-9);
        assertEquals(1.0, FlatBlendMath.blendFactor(160.0, 128.0, 32.0), 1e-9);
        assertEquals(0.5, FlatBlendMath.blendFactor(144.0, 128.0, 32.0), 1e-9);
    }

    @Test void blendFactor_handles_zero_width() {
        assertEquals(0.0, FlatBlendMath.blendFactor(127.9, 128.0, 0.0), 1e-9);
        assertEquals(1.0, FlatBlendMath.blendFactor(128.0, 128.0, 0.0), 1e-9);
    }

    @Test void outside_matches_blend_width() {
        assertFalse(FlatBlendMath.outside(159.9, 128.0, 32.0));
        assertTrue(FlatBlendMath.outside(160.0, 128.0, 32.0));
    }

    @Test void flatDensity_crosses_at_flatY() {
        assertTrue(FlatBlendMath.flatDensity(64.0, 63.0) > 0);
        assertEquals(0.0, FlatBlendMath.flatDensity(64.0, 65.0), 1e-9);
        assertTrue(FlatBlendMath.flatDensity(64.0, 66.0) < 0);
    }

    @Test void blend_endpoints() {
        assertEquals(7.0, FlatBlendMath.blend(7.0, 99.0, 0.0), 1e-9);
        assertEquals(99.0, FlatBlendMath.blend(7.0, 99.0, 1.0), 1e-9);
    }

    @Test void edgeDither_is_bounded_and_deterministic() {
        for (int x = -300; x <= 300; x += 7) {
            for (int z = -300; z <= 300; z += 11) {
                double d = FlatBlendMath.edgeDither(x, z);
                assertTrue(d >= -2.0 && d <= 2.0);
                assertEquals(d, FlatBlendMath.edgeDither(x, z), 1e-12);
            }
        }
    }
}
```

- [ ] **Step 3: 运行测试确认失败**

Run: `gradle test --tests "dev.efm.solaris_progress.worldgen.FlatBlendMathTest"`
Expected: 编译失败（`FlatBlendMath` 不存在）。

- [ ] **Step 4: 实现 `FlatBlendMath`**

```java
public final class FlatBlendMath {
    private FlatBlendMath() {}
    public static double smoothstep(double u) { return u * u * (3.0 - 2.0 * u); }
    public static double chebyshev(double dx, double dz) { return Math.max(Math.abs(dx), Math.abs(dz)); }
    public static double blendFactor(double d, double halfExtent, double blendWidth) {
        if (blendWidth <= 0.0) return d < halfExtent ? 0.0 : 1.0;
        double u = (d - halfExtent) / blendWidth;
        if (u <= 0.0) return 0.0;
        if (u >= 1.0) return 1.0;
        return smoothstep(u);
    }
    public static boolean outside(double d, double halfExtent, double blendWidth) {
        return d >= halfExtent + blendWidth;
    }
    public static double flatDensity(double flatY, double blockY) { return flatY + 1.0 - blockY; }
    public static double blend(double flat, double vanilla, double t) { return flat + (vanilla - flat) * t; }
    public static double edgeDither(int x, int z) {
        int h = x * 374761393 + z * 668265263;
        h = (h ^ (h >> 13)) * 1274126177;
        h = h ^ (h >> 16);
        return ((h & 0xFF) / 255.0 - 0.5) * 4.0; // [-2, 2]
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

Run: `gradle test --tests "dev.efm.solaris_progress.worldgen.FlatBlendMathTest"`
Expected: PASS（8 个测试）。

- [ ] **Step 6: 提交**

```bash
git add build.gradle src/main/java/dev/efm/solaris_progress/worldgen/FlatBlendMath.java src/test/java/dev/efm/solaris_progress/worldgen/FlatBlendMathTest.java
git commit -m "feat(worldgen): add FlatBlendMath + JUnit setup"
```

---

### Task 2: `SolaConfig` + 注册

**Files:**
- Create: `src/main/java/dev/efm/solaris_progress/SolaConfig.java`
- Modify: `src/main/java/dev/efm/solaris_progress/SolaProgress.java`

**Interfaces:**
- Produces: `SolaConfig.SPEC`（`ModConfigSpec`）、静态快照 `enabled`(boolean)`、`flatY`/`halfExtent`/`blendWidth`/`centerX`/`centerZ`(double)`；`SolaConfig::onLoad(ModConfigEvent)`。
- Consumes: 无。

- [ ] **Step 1: 实现 `SolaConfig`**

```java
public final class SolaConfig {
    public static final ModConfigSpec SPEC;
    private static final ModConfigSpec.BooleanValue ENABLED;
    private static final ModConfigSpec.IntValue FLAT_Y, HALF_EXTENT, BLEND_WIDTH, CENTER_X, CENTER_Z;

    public static volatile boolean enabled = true;
    public static volatile double flatY = 64.0, halfExtent = 128.0, blendWidth = 32.0, centerX = 0.0, centerZ = 0.0;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("spawn_flatland");
        ENABLED      = b.comment("启用原点平坦平原 worldgen").define("enabled", true);
        FLAT_Y       = b.comment("地表固体顶面 Y，必须 > 海平面 63").defineInRange("flatY", 64, 64, 300);
        HALF_EXTENT  = b.comment("正方形核心半边长").defineInRange("halfExtent", 128, 16, 1024);
        BLEND_WIDTH  = b.comment("核心外过渡带宽").defineInRange("blendWidth", 32, 0, 512);
        CENTER_X     = b.comment("中心 X").define("centerX", 0);
        CENTER_Z     = b.comment("中心 Z").define("centerZ", 0);
        b.pop();
        SPEC = b.build();
    }

    private SolaConfig() {}

    public static void refresh() {
        enabled = ENABLED.get();
        flatY = FLAT_Y.get();
        halfExtent = HALF_EXTENT.get();
        blendWidth = BLEND_WIDTH.get();
        centerX = CENTER_X.get();
        centerZ = CENTER_Z.get();
    }

    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() == SPEC) refresh();
    }
}
```

- [ ] **Step 2: 在 `SolaProgress` 注册**

在构造函数里 `FTBEvents.init();` 之后加：

```java
        context.registerConfig(ModConfig.Type.COMMON, SolaConfig.SPEC);
        context.getModEventBus().addListener(SolaConfig::onLoad);
```

（`defineInRange("flatY", 64, 64, 300)` 已保证 `flatY >= 64 > 63`；`halfExtent`/`blendWidth` 下限非负，满足 Review Focus 5。）

- [ ] **Step 3: 编译验证**

Run: `gradle compileJava`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: 提交**

```bash
git add src/main/java/dev/efm/solaris_progress/SolaConfig.java src/main/java/dev/efm/solaris_progress/SolaProgress.java
git commit -m "feat(worldgen): add SolaConfig with flatland parameters"
```

---

### Task 3: `FlatBlendDensityFunction` + `wrapRouter`

**Files:**
- Create: `src/main/java/dev/efm/solaris_progress/worldgen/FlatBlendDensityFunction.java`
- Test: `src/test/java/dev/efm/solaris_progress/worldgen/FlatBlendDensityFunctionTest.java`

**Interfaces:**
- Consumes: `FlatBlendMath`（Task 1）、`SolaConfig`（Task 2）。
- Produces: 构造器 `FlatBlendDensityFunction(DensityFunction vanilla, double centerX, double centerZ, double flatY, double halfExtent, double blendWidth)`；`static NoiseRouter wrapRouter(NoiseRouter router)`；实现 `DensityFunction`（`compute/fillArray/mapAll/minValue/maxValue/codec`）。

- [ ] **Step 1: 写失败的测试**

`FlatBlendDensityFunctionTest.java`（用测试替身，不依赖注册表）：

```java
package dev.efm.solaris_progress.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FlatBlendDensityFunctionTest {

    static final class FakeDensity implements DensityFunction {
        @Override public double compute(DensityFunction.FunctionContext c) { return 10.0; }
        @Override public void fillArray(double[] a, DensityFunction.ContextProvider p) {
            for (int i = 0; i < a.length; i++) a[i] = compute(p.forIndex(i));
        }
        @Override public DensityFunction mapAll(DensityFunction.Visitor v) { return v.apply(this); }
        @Override public double minValue() { return -1.0; }
        @Override public double maxValue() { return 1.0; }
        @Override public KeyDispatchDataCodec<? extends DensityFunction> codec() {
            throw new UnsupportedOperationException();
        }
    }

    private static DensityFunction.FunctionContext ctx(int x, int y, int z) {
        return new DensityFunction.SinglePointContext(x, y, z);
    }

    @Test void outside_returns_vanilla_untouched() {
        FlatBlendDensityFunction f = new FlatBlendDensityFunction(new FakeDensity(), 0, 0, 64, 128, 32);
        assertEquals(10.0, f.compute(ctx(1000, 0, 1000)), 1e-9);
    }

    @Test void inside_returns_flat_density() {
        FlatBlendDensityFunction f = new FlatBlendDensityFunction(new FakeDensity(), 0, 0, 64, 128, 32);
        // flatDensity(64, 50) = 15
        assertEquals(15.0, f.compute(ctx(0, 50, 0)), 1e-9);
    }

    @Test void mapAll_recurses_into_vanilla() {
        FakeDensity vanilla = new FakeDensity();
        FlatBlendDensityFunction f = new FlatBlendDensityFunction(vanilla, 0, 0, 64, 128, 32);
        boolean[] visitedVanilla = {false};
        DensityFunction mapped = f.mapAll(df -> { if (df == vanilla) visitedVanilla[0] = true; return df; });
        assertTrue(visitedVanilla[0], "visitor 必须被应用到内部原版密度");
        assertTrue(mapped instanceof FlatBlendDensityFunction);
    }

    @Test void minMax_are_conservative() {
        FlatBlendDensityFunction f = new FlatBlendDensityFunction(new FakeDensity(), 0, 0, 64, 128, 32);
        assertTrue(f.minValue() <= -1.0);
        assertTrue(f.maxValue() >= 1.0);
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

Run: `gradle test --tests "dev.efm.solaris_progress.worldgen.FlatBlendDensityFunctionTest"`
Expected: 编译失败（`FlatBlendDensityFunction` 不存在）。

- [ ] **Step 3: 实现 `FlatBlendDensityFunction`**

关键点（完整 body 由签名与测试决定，此处只钉死算法与 record 顺序）：

- `compute`：`d=FlatBlendMath.chebyshev(ctx.blockX()-centerX, ctx.blockZ()-centerZ)`；
  `outside` 时 `return vanilla.compute(ctx)`；否则
  `t=blendFactor(...)`，`return FlatBlendMath.blend(FlatBlendMath.flatDensity(flatY, ctx.blockY()), vanilla.compute(ctx), t)`。
- `fillArray`：`for (int i=0;i<a.length;i++) a[i]=compute(p.forIndex(i));`
- `mapAll`：`DensityFunction mapped = vanilla.mapAll(v); return v.apply(new FlatBlendDensityFunction(mapped, centerX, centerZ, flatY, halfExtent, blendWidth));`
- `minValue`：`Math.min(vanilla.minValue(), flatY + 1.0 - 320.0)`。
- `maxValue`：`Math.max(vanilla.maxValue(), flatY + 1.0 + 64.0)`。
- `codec`：`throw new UnsupportedOperationException("runtime-only density function");`
- `wrapRouter(NoiseRouter router)`：按 `NoiseRouter` 15 参 record 顺序（`NoiseRouter.java:7`）构造新 router，
  仅把第 11 个 `initialDensityWithoutJaggedness` 与第 12 个 `finalDensity` 换成包一层的实例；
  `centerX/centerZ/flatY/halfExtent/blendWidth` 取自 `SolaConfig` 快照。

- [ ] **Step 4: 运行测试确认通过**

Run: `gradle test --tests "dev.efm.solaris_progress.worldgen.FlatBlendDensityFunctionTest"`
Expected: PASS（4 个测试）。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/dev/efm/solaris_progress/worldgen/FlatBlendDensityFunction.java src/test/java/dev/efm/solaris_progress/worldgen/FlatBlendDensityFunctionTest.java
git commit -m "feat(worldgen): add FlatBlendDensityFunction + router wrapping"
```

---

### Task 4: `RandomStateAccessor` + `ChunkMapMixin` + mixin 注册

**Files:**
- Create: `src/main/java/dev/efm/solaris_progress/mixins/RandomStateAccessor.java`
- Create: `src/main/java/dev/efm/solaris_progress/mixins/ChunkMapMixin.java`
- Modify: `src/main/resources/solaris_progress.mixins.json`（`mixins` 数组加入 `ChunkMapMixin`、`RandomStateAccessor`）

**Interfaces:**
- Consumes: `FlatBlendDensityFunction.wrapRouter`（Task 3）、`SolaConfig.enabled`（Task 2）。
- Produces: 主世界 `RandomState.router` 在 `ChunkMap` 构造末尾被替换。

- [ ] **Step 1: 实现 `RandomStateAccessor`**

```java
@Mixin(RandomState.class)
public interface RandomStateAccessor {
    @Mutable @Accessor("router") void solaris$setRouter(NoiseRouter router);
    @Accessor("router") NoiseRouter solaris$getRouter();
}
```

- [ ] **Step 2: 实现 `ChunkMapMixin`**

```java
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Mutable @Shadow private RandomState randomState;
    @Shadow final ServerLevel level;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void solaris$wrapOverworldRouter(CallbackInfo ci) {
        if (!SolaConfig.enabled) return;
        if (level.dimension() != Level.OVERWORLD) return;
        RandomStateAccessor acc = (RandomStateAccessor) (Object) this.randomState;
        acc.solaris$setRouter(FlatBlendDensityFunction.wrapRouter(acc.solaris$getRouter()));
    }
}
```

- [ ] **Step 3: 登记到 `solaris_progress.mixins.json`**

`"mixins": [ "PlayerListMixin", "ChunkMapMixin", "RandomStateAccessor" ]`。

- [ ] **Step 4: 编译验证**

Run: `gradle compileJava`
Expected: BUILD SUCCESSFUL（mixin 注解处理器通过；refmap 生成）。

- [ ] **Step 5: 提交**

```bash
git add src/main/java/dev/efm/solaris_progress/mixins/RandomStateAccessor.java src/main/java/dev/efm/solaris_progress/mixins/ChunkMapMixin.java src/main/resources/solaris_progress.mixins.json
git commit -m "feat(worldgen): wrap overworld NoiseRouter in ChunkMap"
```

---

### Task 5: `MultiNoiseBiomeSourceMixin` + mixin 注册

**Files:**
- Create: `src/main/java/dev/efm/solaris_progress/mixins/MultiNoiseBiomeSourceMixin.java`
- Modify: `src/main/resources/solaris_progress.mixins.json`（`mixins` 数组加入 `MultiNoiseBiomeSourceMixin`）

**Interfaces:**
- Consumes: `FlatBlendMath.chebyshev` / `edgeDither`（Task 1）、`SolaConfig`（Task 2）。
- Produces: 区域内群系强制为 `plains`。

- [ ] **Step 1: 实现 `MultiNoiseBiomeSourceMixin`**

```java
@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {
    @Shadow public abstract boolean stable(ResourceKey<MultiNoiseBiomeSourceParameterList> key);
    @Shadow public abstract Set<Holder<Biome>> possibleBiomes();

    private static Holder<Biome> solaris$plains;

    @Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"), cancellable = true)
    private void solaris$forcePlains(int x, int y, int z, Climate.Sampler sampler,
                                     CallbackInfoReturnable<Holder<Biome>> cir) {
        if (!SolaConfig.enabled) return;
        if (!this.stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD)) return;
        double bx = x * 4.0, bz = z * 4.0;
        double d = FlatBlendMath.chebyshev(bx - SolaConfig.centerX, bz - SolaConfig.centerZ);
        if (d < SolaConfig.halfExtent + FlatBlendMath.edgeDither(x, z)) {
            Holder<Biome> plains = solaris$plains();
            if (plains != null) cir.setReturnValue(plains);
        }
    }

    private Holder<Biome> solaris$plains() {
        if (solaris$plains == null) {
            for (Holder<Biome> h : this.possibleBiomes()) {
                if (h.unwrapKey().map(k -> k.equals(Biomes.PLAINS)).orElse(false)) {
                    solaris$plains = h;
                    break;
                }
            }
        }
        return solaris$plains;
    }
}
```

- [ ] **Step 2: 登记到 `solaris_progress.mixins.json`**

`"mixins"` 数组追加 `"MultiNoiseBiomeSourceMixin"`。

- [ ] **Step 3: 编译验证**

Run: `gradle compileJava`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 4: 提交**

```bash
git add src/main/java/dev/efm/solaris_progress/mixins/MultiNoiseBiomeSourceMixin.java src/main/resources/solaris_progress.mixins.json
git commit -m "feat(worldgen): force plains biome inside spawn region"
```

---

### Task 6: 出生点强制

**Files:**
- Modify: `src/main/java/dev/efm/solaris_progress/ForgeEvents.java`

**Interfaces:**
- Consumes: `SolaConfig.enabled` / `SolaConfig.flatY`（Task 2）。

- [ ] **Step 1: 增加事件处理器**

在 `ForgeEvents` 内新增（`ForgeEvents` 已由 `SolaProgress` 注册到 `MinecraftForge.EVENT_BUS`）：

```java
    @SubscribeEvent
    public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.OVERWORLD) return;
        if (!SolaConfig.enabled) return;
        event.getSettings().setSpawn(new BlockPos(0, (int) SolaConfig.flatY + 1, 0), 0f);
        event.setCanceled(true);
    }
```

- [ ] **Step 2: 编译验证**

Run: `gradle compileJava`
Expected: BUILD SUCCESSFUL。

- [ ] **Step 3: 提交**

```bash
git add src/main/java/dev/efm/solaris_progress/ForgeEvents.java
git commit -m "feat(worldgen): pin overworld spawn to flat region center"
```

---

### Task 7: 端到端运行验证（新存档）

**Files:** 无（验证任务；若发现问题则回到对应任务修复并提交）。

**Interfaces:**
- Consumes: 全部前序任务。

- [ ] **Step 1: 准备全新世界**

确认/删除旧世界目录：`run/world`（若存在则改名而不是删除，便于对照）。

- [ ] **Step 2: 启动服务端**

Run: `gradle runServer`
Expected: 日志无 `Mixin apply failed` / `InvalidInjectionException`；出现 `Done`；出生点区块在原点生成。

- [ ] **Step 3: 控制台验证地形与群系**

在服务端控制台依次输入：

```
execute if block 0 64 0 minecraft:grass_block
execute if block 400 64 400 minecraft:grass_block
execute if biome 0 64 0 minecraft:plains
execute if biome 400 64 400 minecraft:plains
```

Expected：原点处第一条成功（平坦核心）；第二条应失败（区域外恢复原版，高度不保证 64）；第三条成功；第四条失败。

- [ ] **Step 4: 目视验证（客户端）**

`gradle runClient` 打开该世界，确认：原点 256×256 是平整草地、有树/花草、边界平滑过渡、区域外与 vanilla 一致、地下有洞穴与矿脉；出生点在 `(0,65,0)`。

- [ ] **Step 5: 关闭并收尾**

停止服务端/客户端；若 Step 3/4 发现问题，回到对应 task 修复、重测并提交；否则本任务无需提交。

---

## Self-Review

- **Spec coverage**：4.1→Task 1、4.2→Task 3、4.3→Task 4、4.4→Task 5、4.5→Task 6、4.6→Task 2、测试计划→Task 7。无遗漏。
- **Step scan**：每个 Step 单一动作；实现步骤只给签名/算法，未转录整段程序。
- **Type consistency**：`halfExtent`/`blendWidth`/`flatY`/`centerX`/`centerZ` 在 Config、DensityFunction、Biome mixin、Spawn handler 中命名一致；`wrapRouter`/`solaris$setRouter`/`solaris$getRouter` 一致。
- **Review Focus**：#1/#2 由 Task 1 测试、#3 由代码审查 + Task 7 运行验证（纯 JUnit 无法加载 MC 密度类，已 ledger 说明）、#4/#5 由 Task 2 校验 + Task 7 运行验证。
- **Proportion**：计划短于规格体量，未搬运实现细节。
