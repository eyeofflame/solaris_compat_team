# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概览

Gradle 多工程仓库，产出**两个可独立发布的 Forge 1.20.1 mod**（一个 jar 内可含两个 modid）：

| 产物 | modid | 源码包 | 说明 |
| --- | --- | --- | --- |
| `solaris_compat`（根工程） | `solaris_compat` | `dev.efm.solaris_compat` | 整合包兼容/联动补丁集合（Mixin 为主） |
| 同上 jar | `mekanism_agriculture` | `dev.efm.mekanism_agriculture` | 同 jar 第二个 modid：电力驱动神秘农业注魔机 |
| `solaris_rpg`（子工程） | `solaris_rpg` | `dev.efm.rpg` | Galgame 风格 RPG 对话框架，独立发布 |

技术栈：Minecraft 1.20.1 / Forge 47.4.10 / Parchment `2023.09.03-1.20.1` 映射 / Java 17 / ForgeGradle `[6.0.16,6.2)` / MixinGradle 0.7.x / MixinExtras 0.4.1。代码注释与提交信息用中文。

## 常用命令

**本仓库没有 `gradlew` 脚本**（`gradle/wrapper/` 里只有 properties），直接用全局 Gradle（当前 8.14.5）。`gradle.properties` 设了 `org.gradle.daemon=false`。

```bash
gradle build                          # 根工程：solaris_compat + mekanism_agriculture
gradle :solaris_rpg:build             # RPG 子工程
gradle runClient                      # 根工程客户端（工作目录 run/）
gradle runServer                      # 根工程服务端
gradle runData                        # 数据生成 → src/generated/resources
gradle :solaris_rpg:runClient          # RPG 子工程客户端（工作目录 solaris_rpg/run/）
gradle :solaris_rpg:runServer
```

- **没有测试框架**（无 test 源集/任务），不要指望 `gradle test` 能跑。
- `runData` 的输出目录 `src/generated/resources` 是**有意入库**的数据源（见悬赏池示例数据），不是纯构建产物。
- 改 Mixin 后需完整 `build` 重新生成 refmap（`solaris_compat.refmap.json` / `solaris_rpg.refmap.json`）。
- `META-INF/mods.toml` 与 `pack.mcmeta` 里的 `${...}` 占位符由 `processResources` 从 `gradle.properties` 展开，不要写死版本号。

## 架构

### 构建拓扑

- 根工程 = `solaris_compat` + `mekanism_agriculture`（`mods.toml` 中一次声明两个 modid）。
- `solaris_rpg` 是自包含子工程：自己的 `build.gradle`、`run/`、资源、`mods.toml`。它**必须应用 MixinGradle 插件**（自身无 mixin），否则 dev 环境缺少 refmap 重映射，LDLib/KubeJS/Architectury 的 mixin 会 `Mixin apply failed`。
- 两个工程**零编译依赖**，只在元数据层声明关系：`solaris_compat` 把 `solaris_rpg` 列为可选前置（mandatory=false, ordering=AFTER）。
- 跨工程编译期依赖机制是根 `build.gradle` 的自定义 `solarisCompile` configuration（消费别的工程 `build/classes` 的 named 变体）。**不要用普通 `project()`/jar 依赖**——`reobfJar` 会原地覆盖 class 文件为 SRG 名，导致编译失败。当前无条目，属预留。
- 本地 flatDir 依赖在 `libs/`（16 个 jar）与 `libs/coin/`（2 个），都有意入库（flatDir 同时配了这两个目录）；新增第三方库通常是把 jar 丢进 `libs/` 再加 `fg.deobf("libs:name:...")`。

### 主 mod：兼容补丁（Mixin）

`src/main/resources/solaris_compat.mixins.json` 注册，全部在 `dev.efm.solaris_compat.mixin` 下：

- `thermal/`（全部 `remap = false`，目标是 CoFH 未混淆类）：把灌注器专属升级限制在灌注器内且互斥、加工结束时概率增产。**mixin 方法体不直接碰混淆的 Minecraft 成员**，逻辑全部委托给 `api/InsolatorUpgradeHelper`（把 `this` 原样传入）——这是本仓库处理 CoFH mixin 的既定模式。
- `functional_storage/FluidDrawerTileMixin`：`serverTick` TAIL → `api/WaterGenerator.tick`，流体抽屉靠 utility 升级槽自产水。
- `corpseFix/DeathEventsMixin`：`@Redirect` Corpse 的 `addFreshEntity`；玩家死于虚空时把尸体改送重生点。
- `sola_trade/VillagerMixin`：`Villager.setVillagerData` HEAD 抛自定义 `VillagerProfessionUpdateEvent`（Forge 总线，事件带 villager 本体），由 `SolarisCompat.onVillagerProUpdate` 消费——村民获得职业时把对应 `TradeData.toNbt()` 写进 persistentData（键 `sola_trades`），失去职业时移除。
- `sola_trade/ServerPlayerMixin`（取消 `sendMerchantOffers`）与 `sola_trade/MinecraftMixin`（注册在 mixins.json 的 client 列表，取消 `setScreen(MerchantScreen)`）：屏蔽原版村民交易界面。

其余：`common/SRegistry` 注册物品/创造页签/`solaris_shapeless` 配方序列化器；`data/` + `events/BountyCache` 是悬赏数据池管线（datapack registry → 静态缓存，**目前无消费端**）。

村民交易配置：`/sola_export`（`command/SolaExportCommand`，OP 权限）把全部已注册职业导出为 `config/solaris_compat/trade/<命名空间>/<path>.json` 模板（内容只有 `{"profession": id}`）；`data/reader/TradeConfigLoader` 在开服与 `/reload` 时扫描该目录，按 `TradeData.CODEC`（`data/TradeData`，schema 参考 `src/main/resources/data/solaris_compat/example_trade/example.json`）解析并按职业缓存，用 `TradeConfigLoader.get(职业id)` 读取。目录常量 `TradeConfigLoader.TRADE_DIR` 由导出与读取两端共用。

交易界面：原版交易界面被 mixin 屏蔽后改走 LDLib 自绘——`SolarisCompat.onClickVillager` 在玩家空手右键村民时取其 `sola_trades` NBT 开界面；`ldlib/SolaTradeFactory`（UIFactory id `solaris_compat:trade_ui`，构造时注册）用 `TradeData.fromNbt` 还原同步数据；`ldlib/LDSAPI` 加载 LDLib 编辑器工程（`<游戏目录>/ldlib/assets/ldlib/projects/ui/solaris_trade.ui`，**不在仓库里，客户端与服务端各自需要该文件**）并按页签绑定：左侧列表点选、右侧 need/sell 图标与摘要文字、右下按钮经 `ldlib/TradeExecutor` 服务端结算（扣背包、给产物）。`Level.pick` 在职业设定时抽签：`SolarisCompat.onVillagerProUpdate` 每级从 pool 随机抽 `pick` 条（服务端抽签、客户端不计算），结果以 `TradeData.toNbt()` 存进村民 `sola_trades`（pick 字段保留）；交互以该快照为准，无快照的老村民首次交互时现场按配置抽一份。改配置不追溯已有村民——删掉其 `sola_trades` 再交互即可重抽。

### 同 jar 第二 modid：`mekanism_agriculture` 注魔机

`common/registration/TileEntityMekInfusioner`（约 427 行）是核心，几个刻意的设计约束：

- 继承 `TileEntityConfigurableMachine` 而非 `TileEntityElectricMachine`——Mekanism 配方类型无法在外部 mod 扩展，且注魔是 9 进 1 出结构。
- 槽 0 = 核心物（固定匹配 `ingredients[0]`）；槽 1..8 = 辅料，**按数量无序匹配**，用自研回溯 `assignReagents`（同物品槽位剪枝）。刻意不用 `ISpecialRecipe#matches(IItemHandler)` 的 `RecipeMatcher`（一对一匹配，与按数量语义不符）。
- `NineSlotItemHandler`：`matches` 收到的 handler 必须恰好 9 格；直接传机器 capability（9 输入+输出+能量=11 格）会**静默永不匹配**。
- 能量单位是**焦耳**：`AttributeEnergy(1250, 500000)` ≈ 500 FE/t、200 kFE（Mekanism 默认 2.5 J/FE）。
- JEI 集成是登记为神秘农业「注魔」类别的催化剂 + GUI 点击区，槽位序必须 0=核心、1..8=辅料。

### 子工程：`solaris_rpg` 对话框架

- UI 是 LDLib `ModularUI` 全屏界面（`widgets/DialogueRoot`）；对话推进 **100% 在客户端**（`StateEngine` 纯逻辑节点图，无 MC 依赖），零网络往返。
- 界面刷新靠**每 tick 轮询 `StateEngine.revision()`**，按钮回调里只改状态机、不直接增删控件（否则 `ConcurrentModificationException`）。
- 网络只做通知与校验：两个 C2S 包（选选项 / 结束），服务端校验界面开着且选项真实存在后才触发 `DialogueHooks` 与 `DialogueEndEvent`。改包结构要同步提升 `RpgNetwork.VERSION`。
- **KubeJS/Rhino 是可选依赖**：只有 `dev.efm.rpg.kjs` 包可以引用 KubeJS 类；公共代码（网络包、`DialogueHooks`）一律不得引用，靠 `DialogueHooks` 静态回调解耦。
- 剧本三层注册表 `ScriptRegistry`：`BUILTIN`（Java 内置，reload 不冲掉）< `DATAPACK`（`data/<ns>/solaris_rpg/*.json`）< `SCRIPTED`（KubeJS）；reload 时后两层整体替换。
- 控件类**故意不标 `@OnlyIn(Dist.CLIENT)`**，靠运行时 `isRemote()` 判断——RuntimeDistCleaner 会剥离方法但保留调用点，专用服务器会 `NoSuchMethodError`。

## 约定与陷阱（改动前必读）

1. `remap = false` 的 mixin（CoFH 目标）方法体内不能碰 Minecraft 混淆成员；涉 `ItemStack` 的逻辑放普通类（`InsolatorUpgradeHelper` 模式）。
2. `@Invoker getAugmentsAsList()` 每次调用返回新列表，禁止缓存作快照，否则互斥判断失效。
3. 不要用 `@OnlyIn(Dist.CLIENT)` 包裹方法体里引用客户端逻辑的代码（见上）。
4. 灌注器升级的校验有两条路（机器内运行时 + 工匠台给机器物品装），改动要两条都覆盖（`MachineBlockEntityMixin` + `BlockItemAugmentableMixin`）。
5. `docs/` 与 `.claude/` 被 gitignore；`libs/`、`src/generated/`、`agriculture_infusioner.bbmodel`（Blockbench 模型源文件）是**有意入库**，不要加忽略。
6. 所有 `JavaCompile` 强制 UTF-8（源码含中文注释/字符串），保持文件编码一致。
7. `config/` 不是资源包根：往 config 目录里放自定义 json 后，读取端只能自己走文件系统扫描（参考 `TradeConfigLoader` 的 `ResourceManagerReloadListener` + `AddReloadListenerEvent` 接法，开服与 `/reload` 都会触发）；`SimpleJsonResourceReloadListener` 这类加载器只能读 `data/` 下的文件。
8. LDLib 的 `UIFactory.createUITemplate` 在客户端与服务端**各构建一次组件树**，widget 的点击经「子控件索引链」路由到服务端同一实例（不依赖 id）；因此运行时动态增删控件必须两端同步执行（参考 `LDSAPI.refresh`：行按钮回调两端都会跑），只改一端会让后续点击路由错位。`ButtonWidget.setOnPressCallback` 的回调两端都会触发，服务端逻辑要自己判 `player instanceof ServerPlayer`。

## 快速定位与详细文档

关键入口：主 mod `SolarisCompat.java`；注魔机 `TileEntityMekInfusioner.java`；对话 `SolarisRpg.java` + `StateEngine.java` + `widgets/DialogueRoot.java`；剧本格式 `data/ScriptJson.java` + `solaris_rpg/src/main/resources/data/solaris_rpg/solaris_rpg/demo.json`；交易配置 `command/SolaExportCommand.java`（导出）+ `data/reader/TradeConfigLoader.java`（读取）+ `data/TradeData.java`（模型）；交易界面 `ldlib/SolaTradeFactory.java` + `ldlib/SolaTradeHolder.java` + `ldlib/LDSAPI.java`。

`docs/PROJECT_INDEX.md`（被 gitignore，仅本地存在）是按文件的全仓库索引，含完整调用链与"未接线/半成品"清单，需要深入某个子系统时先查它。`docs/superpowers/specs/` 与 `docs/superpowers/plans/` 是 RPG 子工程拆分设计与实现计划（部分结论已过时，如 `solaris_rpg` 前置已改为可选）。
