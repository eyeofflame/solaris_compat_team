# Solaris Progress — 原点平坦平原 worldgen 设计规范

- 日期：2026-10-02
- 状态：已评审，待实施
- 平台：Minecraft 1.20.1 / Forge 47.4.10 / Java 17 / Parchment 2023.09.03-1.20.1
- 相关模块：`dev.efm.solaris_progress`

## 1. 背景与目标

整合包需要一个"以出生点为中心、天然平坦的平原"作为开局据点地基。结构
`src/main/resources/data/solaris_progress/structures/init.nbt` 是一块
**239(X) × 58(Y) × 235(Z)** 的平整聚落（自带草地、无起伏），若直接塞进有起伏的主世界会
产生悬空/埋入/矩形断崖，因此改为**从世界生成层面**把出生点周围做成平坦平原。

**目标**：新建存档时，以原点 `(0, 0)` 为中心、半径 128（即 256×256）的区域内：

1. 地形压平到统一高度 `flatY`；
2. 生物群系强制为 `minecraft:plains`；
3. **保留**自然平原的地表（草/泥）、植被（树、花草）、"经典"洞穴（carver）、矿脉（OreVeinifier）；
4. 出生点强制钉在 `(0, flatY+1, 0)`；
5. 区域外完全保持原版世界生成，边界平滑过渡。

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
"在 `flatY` 交叉的线性密度"，并与原版密度按半径平滑混合。

## 4. 详细设计

### 4.1 `FlatBlendDensityFunction`（核心）

新增 `dev.efm.solaris_progress.worldgen.FlatBlendDensityFunction implements DensityFunction`。

字段：`DensityFunction vanilla`、`double flatY`、`double innerRadius`、`double blendWidth`。

- `compute(FunctionContext ctx)`：
  ```
  d = Math.sqrt(x*x + z*z)          // x=ctx.blockX(), z=ctx.blockZ()
  if (d >= innerRadius + blendWidth) return vanilla.compute(ctx)
  t = smoothstep(clamp((d - innerRadius) / blendWidth, 0, 1))
  flat = flatY + 1 - ctx.blockY()   // y<=flatY 实心，y>flatY 空气
  return lerp(flat, vanilla.compute(ctx), t)
  ```
  - `smoothstep(u)=u*u*(3-2u)`；`lerp(a,b,t)=a+(b-a)*t`。
  - `t=0`（内部）→ 纯平地；`t=1`（混合带外缘）→ 纯原版。
- `mapAll(Visitor v)`：**必须递归**，否则 `NoiseChunk` 无法给内部原版密度挂插值/缓存：
  `return v.apply(new FlatBlendDensityFunction(vanilla.mapAll(v), flatY, innerRadius, blendWidth));`
- `minValue()` / `maxValue()`：返回与原版及平地取并集的保守值
  （平地范围 = `[flatY+1-maxBuildY, flatY+1-minBuildY]`）。
- `fillArray(array, provider)`：按 `provider.forIndex(i)` 逐点 `compute`。
- `codec()`：运行时包裹不需要序列化 → 抛 `UnsupportedOperationException`。
- 实现 `DensityFunction`（不继承 `SimpleFunction`，因为需要自定义 `mapAll` 递归）。

### 4.2 注入点：替换主世界的 `router`

**新增 `mixins/RandomStateAccessor.java`**：
```java
@Mixin(RandomState.class)
public interface RandomStateAccessor {
    @Mutable @Accessor("router") void solaris$setRouter(NoiseRouter router);
    @Accessor("router") NoiseRouter solaris$getRouter();
}
```

**新增 `mixins/ChunkMapMixin.java`**：重定向 `ChunkMap.<init>` 中对
`RandomState.create(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)Lnet/minecraft/world/level/levelgen/RandomState;`
的调用（`ChunkMap.java:165`）：
```java
@Shadow final ServerLevel level;

@Redirect(method = "<init>", at = @At(value = "INVOKE",
    target = "Lnet/minecraft/world/level/levelgen/RandomState;create(Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;Lnet/minecraft/core/HolderGetter;J)Lnet/minecraft/world/level/levelgen/RandomState;"))
private RandomState solaris$wrapOverworldRouter(NoiseGeneratorSettings settings, HolderGetter<?> noises, long seed) {
    RandomState state = RandomState.create(settings, noises, seed);
    if (level.dimension().equals(Level.OVERWORLD) && SolaConfig.enabled) {
        var acc = (RandomStateAccessor) state;
        acc.solaris$setRouter(FlatBlendDensityFunction.wrapRouter(acc.solaris$getRouter()));
    }
    return state;
}
```
- `FlatBlendDensityFunction.wrapRouter(router)` 用 record 全参构造一个新的 `NoiseRouter`，
  仅把 `finalDensity`、`initialDensityWithoutJaggedness` 换成 `FlatBlendDensityFunction`，其余分量原样。
- 选择 `ChunkMap` 而非 `RandomState` 注入：这里能拿到明确的维度 `level`，只影响主世界，
  且兼容 datapack/预设修改过的 overworld 噪声设置。
- `RandomState` 的 `sampler`（温度/湿度/大陆性等）不受影响，群系采样仍用原值（群系由 4.3 单独覆盖）。

### 4.3 生物群系强制

**新增 `mixins/MultiNoiseBiomeSourceMixin.java`**：
```java
@Inject(method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
        at = @At("HEAD"), cancellable = true)
private void solaris$forcePlains(int x, int y, int z, Climate.Sampler sampler, CallbackInfoReturnable<Holder<Biome>> cir) {
    // 仅主世界 source；x,z 为 quart 坐标 → 乘 4 得方块坐标
    if (solaris$isOverworld() && inRegion(x << 2, z << 2)) {
        cir.setReturnValue(solaris$plainsHolder());
    }
}
```
- `solaris$isOverworld()`：`((MultiNoiseBiomeSource)(Object)this).stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD)`。
- `solaris$plainsHolder()`：从 `possibleBiomes()` 缓存筛选
  `holder.unwrapKey() == Biomes.PLAINS` 的 Holder。
- 边界抖动：对半径判定叠加低频坐标哈希偏移（几格量级），避免完美正方形硬边。

### 4.4 出生点强制

在 `ForgeEvents` 增加处理器（注册于 `MinecraftForge.EVENT_BUS`）：
```java
@SubscribeEvent
public static void onCreateSpawnPosition(LevelEvent.CreateSpawnPosition event) {
    if (!(event.getLevel() instanceof ServerLevel sl) || !sl.dimension().equals(Level.OVERWORLD)) return;
    if (!SolaConfig.enabled) return;
    event.getSettings().setSpawn(new BlockPos(0, (int) SolaConfig.flatY + 1, 0), 0f);
    event.setCanceled(true);
}
```
- 此时（`createLevels` 阶段）原点区块尚未强制生成，`prepareLevels` 会随后加载出生区块，
  区块生成时由 4.1/4.2 产出平地，故 `flatY+1` 正确。

### 4.5 配置

新增 `SolaConfig`（Forge `ModConfigSpec`，common）：

| 键 | 默认 | 说明 |
|---|---|---|
| `enabled` | `true` | 总开关 |
| `flatY` | `64` | 平定后地表固体顶面 Y（须 > 海平面 63，避免水面） |
| `innerRadius` | `128` | 平坦核心半径（256×256） |
| `blendWidth` | `32` | 核心外过渡带宽 |
| `centerX` / `centerZ` | `0` / `0` | 区域中心 |

生成时读取一次；修改后需新存档生效。

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
| `worldgen/FlatBlendDensityFunction.java` | 新增 |
| `mixins/ChunkMapMixin.java` | 新增 |
| `mixins/RandomStateAccessor.java` | 新增 |
| `mixins/MultiNoiseBiomeSourceMixin.java` | 新增 |
| `SolaConfig.java` | 新增 |
| `ForgeEvents.java` | 增加出生点处理器 |
| `solaris_progress.mixins.json` | `mixins` 数组加入 3 个 mixin |
| `SolaProgress.java` | 注册 `SolaConfig`（如需） |

## 7. 测试计划

手动（worldgen 难以 GameTest）：

1. `runServer` 或客户端开新世界。
2. 原点附近：表面固体顶面恒为 `flatY`；`/execute if biome ~ ~ ~ minecraft:plains` 成功。
3. 区域外（> 160）：地形恢复原版起伏。
4. 边界：`innerRadius .. innerRadius+blendWidth` 内高度平滑过渡，无断崖。
5. 地下：有经典洞穴与矿脉；确认无地下含水层（预期）。
6. 地表：有草、树、花草。
7. 出生点：新玩家（经 End 流程后）应落在 `(0, flatY+1, 0)`。
8. 边界群系：无正方形硬切边。

## 8. 风险与缓解

| 风险 | 缓解 |
|---|---|
| `minValue/maxValue` 估计不当导致插值/缓存异常 | 与原版取并集，保守 |
| `mapAll` 不递归 → 内部原版密度失去缓存，性能骤降 | 设计中显式递归并 `v.apply(...)` |
| `codec()` 抛异常被调用 | 运行时包裹不进入序列化路径 |
| 与 `solaris_core` 的 `StructureTemplateMixin` 等无冲突 | 二者注入点不同 |
| 仅对新区块生效 | 文档明示需新存档 |

## 9. 后续 / 关联事项（本次不做）

- `init.nbt` 的放置（`ServerStartedEvent` + `SavedData` 一次性），在平地基础上再叠加地基/对齐。
- 结构内含 133 个实体（含 `create:stationary_contraption`、村民 brain 绝对坐标等），
  放置时需单独处理（另立规格）。
