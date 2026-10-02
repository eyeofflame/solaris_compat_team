# Solaris Progress — 原点平坦平原 worldgen 设计规范

- 日期：2026-10-02（修订：区域由圆形改为正方形）
- 状态：已评审，待实施
- 平台：Minecraft 1.20.1 / Forge 47.4.10 / Java 17 / Parchment 2023.09.03-1.20.1
- 相关模块：`dev.efm.solaris_progress`

## 1. 背景与目标

整合包需要一个"以出生点为中心、天然平坦的平原"作为开局据点地基。结构
`src/main/resources/data/solaris_progress/structures/init.nbt` 是一块
**239(X) × 58(Y) × 235(Z)** 的平整聚落（自带草地、无起伏），若直接塞进有起伏的主世界会
产生悬空/埋入/矩形断崖，因此改为**从世界生成层面**把出生点周围做成平坦平原。

**目标**：新建存档时，以原点 `(0, 0)` 为中心、**半边长 128**（即 256×256 正方形）的区域：

1. 地形压平到统一高度 `flatY`；
2. 生物群系强制为 `minecraft:plains`；
3. **保留**自然平原的地表（草/泥）、植被（树、花草）、"经典"洞穴（carver）、矿脉（OreVeinifier）；
4. 出生点强制钉在 `(0, flatY+1, 0)`；
5. 区域外完全保持原版世界生成，边界平滑过渡。

> **区域形状为正方形而非圆形**：结构足迹 239×235 的角点距原点约 181，圆形半径 128
> 覆盖不到结构角落。因此区域判定与混合都用**切比雪夫距离** `d = max(|dx|, |dz|)`，
> 使平坦核心是 256×256 正方形。

**非目标 / 本次不做**：`init.nbt` 的放置（后续独立步骤，见第 9 节）。

## 2. 已核实的约束（1.20.1 源码）

- **出生点由生物群系反推**：`MinecraftServer.setInitialSpawn` 通过
  `randomState().sampler().findSpawnPosition()` 依赖群系选点（`MinecraftServer.java:396`），
  发生在区块生成之后。"以出生点为中心改世界生成"逻辑倒置 → 必须**定死中心（原点）并强制出生点**。
- Forge 钩子 `ForgeEventFactory.onCreateWorldSpawn(...)`（`MinecraftServer.java:395`）由
  `LevelEvent.CreateSpawnPosition`（`@Cancelable`）驱动；取消后走
  `ServerLevelData.setSpawn(...)`。
- **worldgen 改动只对新生成区块生效**，需要开新世界（或删除区域重生成）。
- 矿脉与"经典"洞穴**独立于 `final_density`**：
  `NoiseChunk.java:135` 单独 `OreVeinifier.create(router.veinToggle(), router.veinRidged(), router.veinGap(), ...)`；
  carver 在 surface 之后执行。因此包裹 `final_density` 不会丢失它们。

## 3. 总体架构

```
ChunkMap.<init>  ──(维度=overworld)──►  RandomState.router 被替换为「包裹后的 router」
                                            │
                                            ├─ finalDensity                       → FlatBlend(原值)
                                            └─ initialDensityWithoutJaggedness   → FlatBlend(原值)

NoiseChunk.<init> 使用 router.finalDensity() 决定实心/空气 → 区域内成为 flatY 平地
        │
        ├─ OreVeinifier（矿脉）    ── 独立，保留
        ├─ Aquifer（含水层）        ── 区域内被实心替代（见 5.2）
        └─ SurfaceSystem / carver / feature ── 照常运行 → 自然平原
```

平地的实现方式：把区域内 `finalDensity` 与 `initialDensityWithoutJaggedness` 替换为
"在 `flatY` 交叉的线性密度"，并与原版密度按**切比雪夫距离**平滑混合。

## 4. 详细设计

### 4.1 `FlatBlendMath`（纯数学，可单元测试）

新增 `dev.efm.solaris_progress.worldgen.FlatBlendMath`（无任何 MC 依赖）：

```java
static double smoothstep(double u);                                  // u*u*(3-2u)
static double chebyshev(double dx, double dz);                       // max(|dx|,|dz|)
static double blendFactor(double d, double halfExtent, double blendWidth);
    // blendWidth<=0 ? (d<halfExtent?0:1) : smoothstep(clamp((d-halfExtent)/blendWidth,0,1))
static boolean outside(double d, double halfExtent, double blendWidth); // d >= halfExtent+blendWidth
static double flatDensity(double flatY, double blockY);              // flatY + 1 - blockY
static double blend(double flat, double vanilla, double t);          // flat + (vanilla-flat)*t
static double edgeDither(int x, int z);                              // ±2 格低频抖动
```

### 4.2 `FlatBlendDensityFunction`（核心）

新增 `dev.efm.solaris_progress.worldgen.FlatBlendDensityFunction implements DensityFunction`。

字段：`DensityFunction vanilla`、`centerX`、`centerZ`、`flatY`、`halfExtent`、`blendWidth`。

- `compute(FunctionContext ctx)`：
  ```
  dx = ctx.blockX() - centerX
  dz = ctx.blockZ() - centerZ
  d  = FlatBlendMath.chebyshev(dx, dz)
  if (FlatBlendMath.outside(d, halfExtent, blendWidth)) return vanilla.compute(ctx)
  t    = FlatBlendMath.blendFactor(d, halfExtent, blendWidth)
  flat = FlatBlendMath.flatDensity(flatY, ctx.blockY())   // y<=flatY 实心，y>flatY 空气
  return FlatBlendMath.blend(flat, vanilla.compute(ctx), t)
  ```
  - `t=0`（正方核心内）→ 纯平地；`t=1`（混合带外缘）→ 纯原版。
- `mapAll(Visitor v)`：**必须递归**，否则 `NoiseChunk` 无法给内部原版密度挂插值/缓存：
  `DensityFunction mapped = vanilla.mapAll(v); return v.apply(new FlatBlendDensityFunction(mapped, ...));`
- `minValue()` / `maxValue()`：与原版及平地范围取并集
  （平地范围 = `[flatY+1-320, flatY+1+64]`）。
- `fillArray(array, provider)`：`for i: array[i] = compute(provider.forIndex(i))`。
- `codec()`：运行时包裹不参与序列化 → 抛 `UnsupportedOperationException`。
- `static NoiseRouter wrapRouter(NoiseRouter router)`：用 `NoiseRouter` 的 15 参 record 构造器
  新建一个，仅替换 `initialDensityWithoutJaggedness` 与 `finalDensity` 为包一层的实例，
  其余分量原样（依据 `NoiseRouter.java:7` 的组件顺序）。

### 4.3 注入点：替换主世界的 `router`

**新增 `mixins/RandomStateAccessor.java`**：
```java
@Mixin(RandomState.class)
public interface RandomStateAccessor {
    @Mutable @Accessor("router") void solaris$setRouter(NoiseRouter router);
    @Accessor("router") NoiseRouter solaris$getRouter();
}
```

**新增 `mixins/ChunkMapMixin.java`**：`ChunkMap.randomState` 是 `private final`（`ChunkMap.java:128`），
在构造末尾替换（避免 `RandomState.create` 在 165/167 行同描述符出现两次导致 `@Redirect` 多变匹配）：
```java
@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Mutable @Shadow private RandomState randomState;
    @Shadow final ServerLevel level;   // ChunkMap.java:124

    @Inject(method = "<init>", at = @At("RETURN"))
    private void solaris$wrapOverworldRouter(CallbackInfo ci) {
        if (!SolaConfig.enabled) return;
        if (level.dimension() != Level.OVERWORLD) return;
        RandomStateAccessor acc = (RandomStateAccessor)(Object) this.randomState;
        acc.solaris$setRouter(FlatBlendDensityFunction.wrapRouter(acc.solaris$getRouter()));
    }
}
```
- 选择 `ChunkMap` 而非 `RandomState`：这里能拿到明确的维度 `level`，只影响主世界，
  且兼容 datapack/预设修改过的 overworld 噪声设置。
- `RandomState` 的 `sampler`（温度/湿度/大陆性等）不受影响，群系采样仍用原值（群系由 4.4 单独覆盖）。
- 构造在 `RETURN` 时已用旧 `randomState` 建好 `chunkGeneratorState`（结构放置用 sampler，不受影响）。

### 4.4 生物群系强制

**新增 `mixins/MultiNoiseBiomeSourceMixin.java`**：
```java
@Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
        at = @At("HEAD"), cancellable = true)
private void solaris$forcePlains(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
    // 仅主世界 source；x,z 为 quart 坐标 → 乘 4 得方块坐标
    if (SolaConfig.enabled && solaris$isOverworld()) {
        double bx = x * 4.0, bz = z * 4.0;
        double d = FlatBlendMath.chebyshev(bx - SolaConfig.centerX, bz - SolaConfig.centerZ);
        if (d < SolaConfig.halfExtent + FlatBlendMath.edgeDither(x, z)) {
            cir.setReturnValue(solaris$plainsHolder());
        }
    }
}
```
- `solaris$isOverworld()`：`stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD)`（`MultiNoiseBiomeSource.java:55`）。
- `solaris$plainsHolder()`：从 `possibleBiomes()` 缓存筛选
  `holder.unwrapKey() == Biomes.PLAINS` 的 Holder。

### 4.5 出生点强制

在 `ForgeEvents` 增加处理器（注册于 `MinecraftForge.EVENT_BUS`）：
```java
@SubscribeEvent
public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
    if (!(event.getLevel() instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) return;
    if (!SolaConfig.enabled) return;
    event.getSettings().setSpawn(new BlockPos(0, (int) SolaConfig.flatY + 1, 0), 0f);
    event.setCanceled(true);
}
```
- 此时（`createLevels` 阶段）原点区块尚未强制生成，`prepareLevels` 会随后加载出生区块，
  区块生成时由 4.2/4.3 产出平地，故 `flatY+1` 正确。

### 4.6 配置

新增 `SolaConfig`（Forge `ModConfigSpec`，common），并在加载时把值快照到静态字段供 worldgen 读取：

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 总开关 |
| `flatY` | `64` | 平定后地表固体顶面 Y（须 > 海平面 63） |
| `halfExtent` | `128` | 正方形核心半边长（256×256） |
| `blendWidth` | `32` | 核心外过渡带宽 |
| `centerX` / `centerZ` | `0` / `0` | 区域中心 |

生成时读取快照；修改后需新存档生效。

## 5. 行为与取舍

### 5.1 保留
- 地表草地/泥土（`SurfaceSystem`，按 plains 群系规则）
- 经典洞穴（carver）
- 矿脉（`OreVeinifier`）
- 树木、花草（biome feature）

### 5.2 丢失（"压平"的代价，已与用户确认接受）
- 区域内 **noodle cave / cave entrance**（属于 `final_density` 组合的一部分）被移除；
  经典洞穴仍在。
- 区域内 **含水层（地下湖/岩浆湖）** 被实心石头替代：因为包裹后 `finalDensity` 在
  `flatY` 以下恒正，Aquifer 不再判定流体。
- 3D 地形起伏被压平（这正是目标）。

## 6. 文件清单

| 文件 | 动作 |
|---|---|
| `worldgen/FlatBlendMath.java` | 新增 |
| `worldgen/FlatBlendDensityFunction.java` | 新增 |
| `mixins/ChunkMapMixin.java` | 新增 |
| `mixins/RandomStateAccessor.java` | 新增 |
| `mixins/MultiNoiseBiomeSourceMixin.java` | 新增 |
| `SolaConfig.java` | 新增 |
| `ForgeEvents.java` | 增加出生点处理器 |
| `SolaProgress.java` | 注册配置、监听 `ModConfigEvent` |
| `solaris_progress.mixins.json` | `mixins` 数组加入 3 个 mixin |
| `build.gradle` | 加 `mavenCentral()` + JUnit，`test { useJUnitPlatform() }` |
| `test/.../FlatBlendMathTest.java` | 新增（纯数学 TDD） |

## 7. 测试计划

- **单元测试**（`gradle test`）：`FlatBlendMath` 的 `smoothstep` 端点/单调、`chebyshev`、
  `blendFactor`（内 0 / 外 1 / 中间）、`outside`、`flatDensity`、`blend` 端点、负坐标对称。
- **手动**（worldgen 难以 GameTest）：
  1. `runServer` 开新世界。
  2. 原点附近：`/execute if block 0 64 0 minecraft:grass_block`（或查询表面高度）恒为 `flatY`。
  3. `/execute if biome 0 ~ 0 minecraft:plains` 成功；区域外恢复原版群系/地形。
  4. 边界 `128 .. 160` 高度平滑过渡，无断崖。
  5. 地下有经典洞穴与矿脉；地表有草、树、花草。
  6. 出生点：新玩家经 End 流程后落在 `(0, flatY+1, 0)`。

## 8. 风险与缓解

| 风险 | 缓解 |
|---|---|
| `minValue/maxValue` 估计不当导致插值/缓存异常 | 与原版取并集，保守 |
| `mapAll` 不递归 → 内部原版密度失去缓存，性能骤降 | 显式递归并 `v.apply(...)` |
| `ChunkMap.randomState` 为 final，需 `@Mutable @Shadow` | 计划中显式标注 |
| `codec()` 抛异常被调用 | 运行时包裹不进入序列化路径 |
| 与 `solaris_core` 的 `StructureTemplateMixin` 等无冲突 | 二者注入点不同 |
| 仅对新区块生效 | 文档明示需新存档 |

## 9. 后续 / 关联事项（本次不做）

- `init.nbt` 的放置（`ServerStartedEvent` + `SavedData` 一次性），在平地基础上再叠加地基/对齐。
- 结构内含 133 个实体（含 `create:stationary_contraption`、村民 brain 绝对坐标等），
  放置时需单独处理（另立规格）。
