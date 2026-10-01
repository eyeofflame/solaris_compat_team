# Solaris RPG 子工程拆分设计

日期: 2026-10-01
状态: 待评审（用户已口头通过对话设计，本文档待书面确认）

## 背景与目标

`solaris_compat` 当前是一个单体 Forge 1.20.1 mod，其中混装了两类职责：

1. 兼容 / 联动补丁（Thermal 灌注器强化、功能性存储产水、机械动力×神秘农业注魔机、Corpse 修复、悬赏数据池等）。
2. 一套通用的 Galgame 风格 RPG 对话框架（`dev.efm.rpg`）及其 KubeJS 集成层。

目标是**把 RPG 对话框架从主 mod 中拆出，成为同仓库内的独立 Gradle 子工程**，产出独立 jar、拥有独立 modid，两个 mod 各自独立发布与安装。主 mod 从此对 RPG 代码零编译依赖，仅在元数据层声明前置关系。

拆分的价值：RPG 框架可独立演进、复用于其它整合包/工程；主 mod 只保留兼容补丁职责，边界清晰。

## 决策摘要（已与用户确认）

| 决策点 | 结论 |
|---|---|
| 拆分形态 | 独立 Gradle 子工程（多模块同仓库） |
| 子工程职责 | 完全自包含：自己监听 Mod/Forge 事件、注册自己的命令 |
| mod 身份与发布 | 独立 modid，两个独立 jar，主 mod 声明其为必需前置 |
| modid | `solaris_rpg` |
| 包名 | 核心保留 `dev.efm.rpg`；KJS 层 → `dev.efm.rpg.kjs`；Olivia → `dev.efm.rpg.entity` / `dev.efm.rpg.client` |
| 构建拓扑 | 方案 1：根工程仍是 `solaris_compat`，新增平级子工程 `:solaris_rpg` |
| 模块间依赖 | 零编译依赖，仅 `mods.toml` 前置声明 |
| 边界 | 搬：`dev.efm.rpg`、KJS 层、Olivia 实体/渲染、RPG 专属资源。不搬：悬赏数据池 / `BountyCache` |

## 1. 构建拓扑与接线

- `settings.gradle` 增加一行 `include 'solaris_rpg'`。
- 新增 `solaris_rpg/build.gradle`，独立声明 `plugins`：`eclipse`、`idea`、`net.minecraftforge.gradle`、`org.parchmentmc.librarian.forgegradle`。**不引入 `org.spongepowered.mixin` 插件**（RPG 无 mixin）。
- 公共版本号沿用根 `gradle.properties`（Gradle 子工程自动继承 root 的项目属性）。
- 子工程 `repositories` 中的 `flatDir` 指向 `rootProject.file('libs')`；远程仓库按需重复声明（首版不抽公共脚本）。
- 子工程工作目录为 `solaris_rpg/run`。
- 首版**不**抽 `gradle/forge-mod.gradle`，不引入 `buildSrc`/convention plugin；等模块数增多再升级。
- 两个 `build.gradle` 都自行配置 `minecraft { mappings ... }`、`reobf`、`processResources` 属性替换。

## 2. 边界

### 搬到 `solaris_rpg`
- `dev.efm.rpg` 全部（data 模型、StateEngine、SFactory、SHolder、widgets、network、DialogueHooks）。
- `dev.efm.solaris_compat.kjs` → `dev.efm.rpg.kjs`（`SolarisRPGKubeJSPlugin`、`SolarisDialogueJS`、`SolarisRPGEvents`）。
- Olivia：`dev.efm.solaris_compat.common.entity.OliviaEntity`/`OliviaEntities` → `dev.efm.rpg.entity`；`dev.efm.solaris_compat.client.OliviaRenderer`/`OliviaClientEvents` → `dev.efm.rpg.client`。
- RPG 专属资源（见第 5 节）。

### 留在主 mod
- `api/`（`SHelper`、`WaterGenerator`、`InsolatorUpgradeHelper`）
- `common/items`、`common/recipeType`、`common/SRegistry`
- `common/entity` 与 `client` 中**非** Olivia 的内容
- `config/`（`SolarisConfig`、`ConfigScreen`）
- `data/` + `data/pools`、`events/BountyCache`
- `mixin/` 全部
- `SolarisCompat` 主类（瘦身，移除 RPG 接线）

## 3. RPG 包布局

```
dev.efm.rpg            核心: @Mod 入口 SolarisRpg、StateEngine、SFactory、SHolder、DialogueHooks
dev.efm.rpg.data       Script/Node/Choice/(ScriptBuilder/NodeBuilder/ChoiceBuilder)/ScriptRegistry/ScriptReloadListener/ScriptJson
dev.efm.rpg.widgets    DialogueRoot/ChoicePanel/TypewriterTextWidget/FullScreenGroup/PortraitTextures/SolarisButtonWidget
dev.efm.rpg.network    RpgNetwork/CPacketChoiceSelected
dev.efm.rpg.entity     OliviaEntity/OliviaEntities
dev.efm.rpg.client     OliviaRenderer/OliviaClientEvents
dev.efm.rpg.kjs        SolarisRPGKubeJSPlugin/SolarisDialogueJS/SolarisRPGEvents
```

注：`SolarisButtonWidget` 目前是空类，原样带走。

## 4. RPG mod 自包含入口

新增 `dev.efm.rpg.SolarisRpg`，`@Mod(SolarisRpg.MODID)`，`MODID = "solaris_rpg"`。在其构造中完成现散落于 `SolarisCompat` 的全部 RPG 接线：

- **MOD 总线**：
  - `ScriptRegistry.defaultReg()`
  - `FMLCommonSetupEvent` → `UIFactory.register(SFactory.INSTANCE)`、`RpgNetwork.register()`
- **FORGE 总线**：
  - `AddReloadListenerEvent` → `new ScriptReloadListener()`
  - `RegisterCommandsEvent` → `/std_create <id>` 命令
- **Olivia 注册**：`OliviaEntities` / `OliviaClientEvents` 的 `@Mod.EventBusSubscriber(modid = ...)` 改为 `solaris_rpg`。

主类 `SolarisCompat` 删除：所有 `dev.efm.rpg.*` import、`SFactory`/`SHolder`/`Script`/`ScriptRegistry`/`ScriptReloadListener`/`RpgNetwork` 的引用、`/std_create` 命令注册、`onAddReloadListeners`、`commonSetup` 中的 RPG 部分、`ScriptRegistry.defaultReg()` 调用。保留：`SolarisConfig` 注册、`ConfigScreen` 扩展点、`DataRegistry` 监听、`SRegistry.register`、`BountyCache` 相关。

## 5. 元数据与资源

新增 / 变更文件：

- `solaris_rpg/src/main/resources/META-INF/mods.toml`
  - `modId = "solaris_rpg"`
  - 依赖：`forge`、`minecraft`、`ldlib` 为必需；`kubejs`、`rhino` 为**可选**（mandatory=false, ordering=AFTER）。
- `solaris_rpg/src/main/resources/pack.mcmeta`（新建）。
- 资源搬运并改命名空间：
  - `assets/solaris_rpg/lang/zh_cn.json`、`en_us.json`：只保留 `entity.solaris_rpg.olivia`（键名随 modid 改）。
  - `assets/solaris_rpg/textures/gui/sola_background.png`（源自 `assets/solaris_compat/textures/gui/sola_background.png`）。
  - `assets/solaris_rpg/textures/entity/olivia.png`（源自 `assets/solaris_compat/textures/entity/olivia.png`）。
  - `data/solaris_rpg/solaris_rpg/demo.json`（源自 `data/solaris_compat/solaris_rpg/demo.json`；剧本 id 变 `solaris_rpg:demo`）。
  - `kubejs.plugins.txt`：入口类改为 `dev.efm.rpg.kjs.SolarisRPGKubeJSPlugin`。
- 主 mod `mods.toml` 新增前置：
  ```toml
  [[dependencies.solaris_compat]]
  modId = "solaris_rpg"
  mandatory = true
  versionRange = "[0,)"
  ordering = "AFTER"
  side = "BOTH"
  ```

## 6. 解耦改动清单

| 文件 | 改动 |
|---|---|
| `dev.efm.rpg.network.RpgNetwork` | 去 `SolarisCompat.MODID` + `SHelper`，改用 `SolarisRpg.MODID` 与 `ResourceLocation.fromNamespaceAndPath` |
| `dev.efm.rpg.SFactory` | 同上 |
| `dev.efm.rpg.widgets.DialogueRoot` | 背景路径 → `solaris_rpg:textures/gui/sola_background.png` |
| `dev.efm.rpg.data.ScriptRegistry` | 立绘常量命名空间 → `solaris_rpg:`（另见第 8 节） |
| `data/solaris_rpg/solaris_rpg/demo.json` | 立绘命名空间 → `solaris_rpg:` |
| `dev.efm.rpg.kjs.SolarisRPGKubeJSPlugin` | `filter.allow` → `dev.efm.rpg`、`dev.efm.rpg.kjs` |
| 各处注释 | 更新的旧路径 / 旧包名同步 |

## 7. .gitignore 更新

现有 `.gitignore` 使用**锚定到根**的路径，不会覆盖新子工程的生成目录。新增：

```
# 子工程生成目录
/solaris_rpg/.gradle/
/solaris_rpg/build/
/solaris_rpg/run/
/solaris_rpg/run-data/
```

同时补齐常见的无关目录（保持锚定风格，避免误伤 `libs/` 等已入库目录）：

```
# 编辑器 / 系统
/.vscode/
**/.DS_Store
Thumbs.db
```

`libs/`、`agriculture_infusioner.bbmodel`、`src/generated/` 均为**有意入库**，不得加入忽略。

## 8. 已知问题（不修，仅记录）

内置剧本 `test`（`ScriptRegistry`）与数据包 `demo.json` 引用的立绘为
`solaris_rpg:textures/gui/portrait/olivia.png`，该文件**从未存在**（仓库只有实体贴图 `textures/entity/olivia.png`）。`PortraitTextures.sizeOf` 返回 null 时 `DialogueRoot` 静默隐藏立绘。

本次仅把命名空间改为 `solaris_rpg:`，**不补美术资源**。是否将来指向占位图或补正式立绘，留待后续单独任务。

## 9. 验证计划

1. **构建**：`gradlew :solaris_rpg:build` 与根 `gradlew build` 各自成功产出 jar；检查 `mods.toml` 的占位符已正确展开。
2. **类冲突自查**：确认两个 jar 无同名类（`dev.efm.rpg.*` 与 `dev.efm.solaris_compat.*` 不重叠）。
3. **冒烟（单装 RPG）**：`runServer` / `runClient` 启动 RPG mod，验证 `/std_create solaris_rpg:demo` 可开界面、`/reload` 重载剧本、KubeJS 脚本事件可用（装了 KubeJS 时）。
4. **冒烟（联装）**：主 mod + RPG 两个 jar 同时安装，验证 `mods.toml` 前置关系、加载顺序、无 `NoClassDefFoundError`。
5. **回归**：主 mod 的兼容功能（灌注器强化、产水、注魔机、悬赏池）不受影响。

## 10. 明确不做

- 不搬悬赏数据池 / `BountyCache`。
- 不改变 RPG 现有功能行为（仅搬迁 + 命名空间调整）。
- 不新建美术资源。
- 不引入 `buildSrc` / convention plugin。
- 不为两个 mod 之间建立 Gradle `project()` 依赖。

## 11. 风险

- **ForgeGradle 多工程**：根工程同时是聚合根又是模块，属非常规用法；子工程需自带完整 ForgeGradle 配置。若构建异常，退路是升级到"纯聚合根"拓扑（方案 2）。
- **联装测试成本**：两个独立 jar 的联调需要手动装两份；本设计不提供组合式 run 配置。
- **资源命名空间迁移**：任何外部整合包若硬编码了 `solaris_compat:` 的 RPG 资源路径或剧本 id（如 `solaris_compat:demo`），拆分后会失效，需随更新调整。
