# Disable Natural Ores Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 禁止所有维度新区块中的原版与可识别模组矿石生成，保留普通地层并支持额外名单热重载。

**Architecture:** 使用共享矿石判定器、独立 JSON 配置和三个生成拦截层。标准矿脉在目标检查处拒绝矿石，大型矿脉包装噪声结果，WorldGenRegion 写入入口补充覆盖自定义 Feature 和结构；不修改普通 Level 写入。

**Tech Stack:** Java 17、Minecraft 1.20.1、Forge 47.4.10、Parchment 2023.09.03-1.20.1、Mixin 0.8.5、Gson、Gradle。

**Spec:** `docs/superpowers/specs/2026-10-02-disable-natural-ores-design.md`

## Global Constraints

- 对所有维度中新生成的区块生效，覆盖原版和可识别的模组矿石。
- 不扫描或删除已经生成的矿石，不阻止玩家放置，也不影响物品、合成或现有法术生产链。
- 不以方块名称包含 `ore` 为判定依据。
- 保留泥土、砂砾、花岗岩、闪长岩、安山岩及大型矿脉中的普通岩石。
- 不通过生成后扫描所有区块来弥补边界，不新增跨区块清除任务。
- `/solaris_resources_reload` 继续需要 2 级权限，支持控制台。
- 配置使用 `config/solaris_resources/worldgen.json` 和 `additionalOreBlocks`，不改变原子配置格式。
- 实现前用 using-git-worktrees 创建隔离工作区；不移动或清理用户原有存档。

## Review Focus

1. 标签重载后不使用旧缓存：Task 2 动态标签输入测试，Task 4 实际 `/reload` 检查。
2. 混合矿石/普通方块 Feature 不能整体取消：Task 3 混合目标测试。
3. 正常游玩或 `/place feature` 不能因标准 Feature 注入被当成新区块生成：Task 3 非 WorldGenRegion 放行测试。
4. 配置错误和生成线程并发不产生半更新：Task 1 保留完整旧集合与并发快照测试。
5. 大型矿脉禁掉粗矿块但不产生空气洞、不全局禁粗矿块：Task 3 返回值测试和 Task 4 生成检查。

## 已核对的入口

参考 Gradle 缓存中的 `forge-1.20.1-47.4.10_mapped_parchment_2023.09.03-1.20.1-sources.jar`：

- `OreFeature.doPlace` 通过 `LevelChunkSection.setBlockState` 直接写入，调用静态 `OreFeature.canPlaceOre` 判断每个目标。
- `ScatteredOreFeature.place` 也调用该判断方法，再调用 `WorldGenLevel.setBlock`。
- `OreVeinifier.create(DensityFunction, DensityFunction, DensityFunction, PositionalRandomFactory)` 返回 `NoiseChunk.BlockStateFiller`，其 `calculate(DensityFunction.FunctionContext)` 返回可空 BlockState。
- `WorldGenRegion.setBlock(BlockPos, BlockState, int, int)` 是补充写入入口。
- 本地 Ars Nouveau 配置 Feature 只有树、浆果和灯等，未发现矿石 Feature。验收使用独立测试命名空间的数据包，不新增模组产品依赖。

## 文件与职责

Java 路径前缀为 `src/main/java/dev/efm/solaris_resources/`。

- 新建 `common/config/WorldgenConfigStore.java`：JSON 读写、校验及不可变名单发布；不依赖 Minecraft 类，便于测试。
- 新建 `common/worldgen/OreGenerationPolicy.java`：原版显式名单及纯判定组合。
- 新建 `common/worldgen/OreGenerationRules.java`：Forge 方块/标签与配置适配，提供生成拦截使用的方法。
- 新建 `mixin/OreFeatureMixin.java`、`mixin/ScatteredOreFeatureMixin.java`、`mixin/OreVeinifierMixin.java`、`mixin/WorldGenRegionMixin.java`：分别实现三个拦截层。
- 修改 `SolarisResources.java`、`common/command/ReloadAtomConfigCommand.java`：启动加载和指令重载。
- 修改 Mixin JSON、中英文语言文件、`build.gradle`、`README.md`：注册、反馈、测试任务和说明。
- 新建对应纯 Java 测试与 Forge GameTest；测试资源限定于 `src/test`。

### Task 1: 世界生成配置存储

**Files:** 创建 `common/config/WorldgenConfigStore.java`、`src/test/java/dev/efm/solaris_resources/common/config/WorldgenConfigStoreTest.java`；修改 `build.gradle`。

**Interfaces:** 构造器 `WorldgenConfigStore(Path file, Predicate<String> validBlock, BiConsumer<Path, Exception> logError)`；提供 `boolean reload()`、`Set<String> additionalOreBlocks()`。初始集合为空，成功发布 `Set.copyOf` 到 volatile 字段。

- [ ] 写失败测试：缺失文件生成 `{"additionalOreBlocks":[]}`；有效文件 `{"additionalOreBlocks":["test:tin_ore","test:tin_ore"]}` 返回 true、集合大小为 1；读取不覆盖文件。
- [ ] 写失败测试：`null`、`{}`、数组外类型、数组中非字符串、非法/未知 ID 均返回 false，保留原完整集合并调用错误日志；缺失文件重建为空集合。
- [ ] 写失败测试：保留的旧快照在后续重载后不改变；多线程重复读取时只可能观察完整旧集合或完整新集合，集合不可修改。
- [ ] 添加 `testWorldgenConfig` JavaExec（沿用 `testAtomConfig`，mainClass 为新测试类）并接入 `check`；运行 `gradle testWorldgenConfig --console=plain`，确认因未实现 API 而失败。
- [ ] 实现读写：UTF-8、pretty JSON、CREATE_NEW、不覆盖已有文件，严格数组元素校验；先解析全量再一次发布，失败不发布。
- [ ] 运行 `gradle testWorldgenConfig testAtomConfig test --console=plain`，要求所有断言通过。
- [ ] 仅提交上述文件：`feat: add reloadable additional ore block configuration`。

### Task 2: 共享矿石规则和重载集成

**Files:** 创建 `common/worldgen/OreGenerationPolicy.java`、`OreGenerationRules.java`、`src/test/java/dev/efm/solaris_resources/common/worldgen/OreGenerationPolicyTest.java`；修改入口、指令、语言文件、`build.gradle`。

**Interfaces:** Policy 提供 `static boolean isOre(String blockId, boolean taggedOre, Set<String> additional)`；Rules 提供 `static boolean isOre(BlockState state)`、`static boolean reload()`、`static BlockState filterVeinResult(BlockState state)`（允许 null）。

- [ ] 写失败测试：煤/深板岩煤、铁/深板岩铁、铜/深板岩铜、金/深板岩金、红石/深板岩红石、青金石/深板岩青金石、钻石/深板岩钻石、绿宝石/深板岩绿宝石、下界金、下界石英、远古残骸全部为 true。
- [ ] 写失败测试：`test:tin_ore` 通过标签或额外集合为 true；`test:ore_decoration` 无上述来源为 false；泥土、砂砾、三种普通岩石、凝灰岩、粗铁块和粗铜块的普通判定为 false。
- [ ] 写失败测试：同一方块 taggedOre 从 true 改为 false 且不在其他名单时判定为 false；补充集合替换后旧目标不再被禁。
- [ ] 添加 `testOrePolicy` JavaExec 并接入 check，运行 `gradle testOrePolicy --console=plain` 观察失败。
- [ ] 实现显式原版集合；Rules 每次使用 `state.is(Tags.Blocks.ORES)`，不缓存标签；配置验证使用 ResourceLocation 和 ForgeRegistries.BLOCKS.containsKey。
- [ ] 启动 commonSetup 的 enqueueWork 内独立加载原子及世界生成配置；指令保留原子计数反馈，新增世界生成加载状态反馈，两类均尝试后决定命令返回值，任一失败返回 0。
- [ ] 在 Task 4 增加指令 GameTest：原子错误不阻止世界生成更新；世界生成错误不阻止原子更新；权限不足不执行更新。
- [ ] 运行 `gradle testOrePolicy testWorldgenConfig testAtomConfig test build --console=plain`。
- [ ] 仅提交本任务文件：`feat: integrate ore recognition and config reload`。

### Task 3: 生成拦截与运行集成测试

**Files:** 创建四个 Mixin、`src/test/java/dev/efm/solaris_resources/common/worldgen/OreGenerationGameTests.java`；修改 Mixin JSON 和 `build.gradle`。

**Interfaces:** 消费 Task 2 的 `isOre(BlockState)` 和 `filterVeinResult(BlockState)`；不添加其他矿石名单。

- [ ] 写 Forge GameTest：通过实际 OreFeature.canPlaceOre 调用链验证矿石拒绝、岩石放行、矿石在前且普通方块在后的混合配置仍能放置普通方块；直接在 ServerLevel 使用 Feature 保持原行为。
- [ ] 写 GameTest：WorldGenRegion 写矿石返回 false 且原方块不变，普通方块成功；使用独立新测试区块构造区域，禁止包装已有游戏存档。
- [ ] 写 GameTest：大型矿脉过滤的 null 保持 null；矿石、粗铁、粗铜结果变为 null（由噪声生成器回退正常基底）；granite/tuff 保持原状态；普通 `isOre(raw_iron_block)` 为 false。
- [ ] 将测试 sourceSet 接入 Forge `gameTestServer` 的 mods sources，游戏测试输出/工作目录设为独立 `build/ore-gametest`；按 Forge 的 GameTest 注册方式注册测试，空模板仅放测试资源。运行 `gradle runGameTestServer --console=plain`，观察拦截行为断言失败而不是只缺少测试注册。
- [ ] 在 OreFeature.doPlace 与 ScatteredOreFeature.place 重定向各自的 `OreFeature.canPlaceOre` 调用：取得当前生成 Level，仅当它为 WorldGenRegion 且目标 state 为矿石时返回 false，否则调用原方法。保留单目标粒度与普通运行时 Feature；不用全局注入静态 canPlaceOre。
- [ ] 在 OreVeinifier.create RETURN 包装原 BlockStateFiller，调用原 calculate 后经 filterVeinResult 返回；只过滤该分支的矿石与粗铁/粗铜，null 不改为空气。
- [ ] WorldGenRegion.setBlock 四参数方法 HEAD 注入：矿石则返回 false，其余继续原行为。仅此生成类，不注入 Level/ServerLevel/Chunk 全局写入。
- [ ] 将四个 Mixin 加入公共 mixins 数组，注入 require=1；逐个核对描述符与 remap 的 refmap 结果，保持原类调用，不引入 MixinExtras 新依赖。
- [ ] 运行 `gradle test build runGameTestServer --console=plain`；要求 GameTest 全通过，日志无 Mixin 注入错误，已有纯测试也通过。
- [ ] 提交：`feat: suppress natural ore placement and noise veins`。

### Task 4: 数据包兼容验收与交付文档

**Files:** 创建 `src/test/resources/data/solaris_resources_test/` 下测试 Feature/placed feature/结构模板与标签（由测试将目标方块加入 forge:ores）；扩展 GameTests；修改 README 与计划复选框。

- [ ] 写测试数据包：用 Ars Nouveau 已注册的 `ars_nouveau:source_gem_block` 作为受控“模组矿石”测试目标，确认 ID 实际存在后使用；仅测试标签将其加入 forge:ores，不改变产品数据包标签。
- [ ] 验证带标签的该方块在标准 Feature 和 WorldGenRegion 写入被阻止；另一无标签目标经 additionalOreBlocks 热重载后被阻止，移除后放行。
- [ ] 验证数据包标签重新加载后新生成检查使用新标签；标签测试前后恢复测试标签，避免测试之间污染。
- [ ] 执行 Task 2 的指令失败隔离和权限测试；通过控制台验证无斜杠调用，游戏 OP 验证有斜杠调用。
- [ ] 在独立临时测试世界用固定种子生成主世界与下界区域，检查原版矿石缺失、普通岩层存在；为大型矿脉选择原始噪声结果存在矿石/粗矿块的坐标检查回退基底，不能用一个本来就没有矿脉的区域宣称覆盖。
- [ ] 在测试区正常玩家放置矿石并确认保留；保存加载含矿石的已生成测试区块，确认未触发清理；验证混合目标、结构矿石写入及非矿石结构方块。
- [ ] README 记录配置格式、命令、标签、旧区块边界、并发重载不追溯在途生成、特殊模组绕过入口的限制。明确 Ars Nouveau 样本是人工测试矿石，不是它自带的矿石 Feature。
- [ ] 运行 `gradle testWorldgenConfig testOrePolicy testAtomConfig test build runGameTestServer --console=plain` 和 `git diff --check`。记录日志路径与 GameTest 数量；若无法游戏内运行，明确列未验证项，不声称全部覆盖。
- [ ] 使用 requesting-code-review 做最终独立审查，处理重要问题后重跑相关验证。
- [ ] 提交本任务文件：`test: verify ore suppression compatibility and document limits`；报告产物路径、验证结果和未验证边界，不自动合并工作区。

## 自查与执行交接

- [x] 设计需求分别落到配置、规则/指令、生成拦截和兼容验收四个任务。
- [x] 入口依据当前映射源码确认，直接区块写入和正常 Level 放行均有独立覆盖。
- [x] 共享方法签名统一，配置原子发布和标签动态读取责任明确。
- [x] 五项 Review Focus 均有对应测试；没有以源代码字符串断言代替行为测试。
- [x] 用户审阅实现计划并选择 Native 执行方式。

建议采用 Native：四项任务的接口紧密相连，当前会话已掌握源码入口，可自行顺序实施并在结束时安排一次独立审查。若选择 Subagent-driven，则每项任务由新代理实现并单独审查。
