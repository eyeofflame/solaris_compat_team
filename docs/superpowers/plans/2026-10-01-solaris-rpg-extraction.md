# Solaris RPG 子工程拆分 实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 RPG 对话框架(核心 + KubeJS 兼容层 + Olivia + 专属资源)从 `solaris_compat` 拆成同仓库内独立 Gradle 子工程 `solaris_rpg`,产出独立 modid 的 jar。

**Architecture:** 根工程仍是 `solaris_compat` mod,新增平级子工程 `:solaris_rpg`(`settings.gradle` include)。两个 mod 零编译依赖,主 mod 仅在 `mods.toml` 把 `solaris_rpg` 声明为必需前置。RPG 子工程自包含:自己的 `@Mod` 入口负责 UI 工厂、网络频道、reload 监听、命令与事件订阅。

**Tech Stack:** Minecraft 1.20.1 / Forge 47.4.10 / ForgeGradle 6 / Parchment / LDLib / KubeJS(可选) / Gradle 多工程。

**Spec:** `docs/superpowers/specs/2026-10-01-solaris-rpg-extraction-design.md`

## Global Constraints

- modid = `solaris_rpg`;主 modid 保持 `solaris_compat`。
- 包名:核心保留 `dev.efm.rpg`;KJS 层 = `dev.efm.rpg.kjs`;Olivia 实体 = `dev.efm.rpg.entity`;Olivia 渲染 = `dev.efm.rpg.client`。
- 两 mod **不得**建立 Gradle `project()` 依赖,只在 `mods.toml` 声明前置。
- RPG 子工程**不**应用 mixin 插件,不含 mixin,不需 `mixinextras`。
- 版本号沿用根 `gradle.properties`(`mod_version=1.1.2.a` 等),子工程自动继承。
- 语言/编码:`options.encoding = 'UTF-8'`。
- 不搬悬赏数据池 / `BountyCache`;不新建美术资源;不引入 `buildSrc`/convention plugin;不改 RPG 现有功能行为。
- 已入库、不得忽略:`libs/`、`agriculture_infusioner.bbmodel`、`src/generated/`。
- 本项目**无单元测试框架**(无 test source set)。因此每个任务的"测试"= 可判定的构建/静态检查;游戏内冒烟为人工观察项,不作自动门禁。

## Review Focus

按最可能坑到使用者的顺序,逐条在此固定检查(每条在所属任务里都有对应检查步骤):

1. **主 mod 残留 `dev.efm.rpg` / `dev.efm.solaris_compat.kjs` 引用** → 主 mod 编译失败。Task 2 用 `git grep` 断言主 `src/main/java` 中零匹配。
2. **资源命名空间残留 `solaris_compat:`** → 纹理/数据包静默丢失。Task 2、Task 3 用 `git grep "solaris_compat:"` 核对 RPG 代码与资源。
3. **KubeJS 未被硬依赖** → 未装 KubeJS 时不得 `NoClassDefFoundError`。Task 2 检查核心包(`dev.efm.rpg` 非 kjs)不 import 任何 `dev.latvian.*`。
4. **`mods.toml` 依赖/占位符错误** → mod 加载报错。Task 1、Task 2 用 `jar --list`/解压核对展开结果。
5. **两 jar 同名类冲突** → 安装即崩。Task 4 交叉比对两 jar 的 class 列表。
6. **`entity.solaris_rpg.olivia` 语言键迁移遗漏** → 实体显示为原始翻译键。Task 3 断言主 lang 已移除、RPG lang 已存在。

---

## File Structure

**新增(工程骨架)**
- `settings.gradle` — 增加 `include 'solaris_rpg'`
- `solaris_rpg/build.gradle` — 子工程 ForgeGradle 配置
- `solaris_rpg/src/main/resources/META-INF/mods.toml`
- `solaris_rpg/src/main/resources/pack.mcmeta`
- `solaris_rpg/src/main/java/dev/efm/rpg/SolarisRpg.java` — `@Mod` 入口

**移动(Java,`git mv`)**
- `src/main/java/dev/efm/rpg/**` → `solaris_rpg/src/main/java/dev/efm/rpg/**`(包名不变)
- `src/main/java/dev/efm/solaris_compat/kjs/**` → `solaris_rpg/src/main/java/dev/efm/rpg/kjs/**`(改包声明)
- `.../solaris_compat/common/entity/Olivia{Entity,Entities}.java` → `dev/efm/rpg/entity/`
- `.../solaris_compat/client/Olivia{Renderer,ClientEvents}.java` → `dev/efm/rpg/client/`

**移动(资源,`git mv`)**
- `assets/solaris_compat/textures/gui/sola_background.png` → `solaris_rpg/.../assets/solaris_rpg/textures/gui/`
- `assets/solaris_compat/textures/entity/olivia.png` → `.../assets/solaris_rpg/textures/entity/`
- `data/solaris_compat/solaris_rpg/demo.json` → `.../data/solaris_rpg/solaris_rpg/`
- `kubejs.plugins.txt` → `solaris_rpg/src/main/resources/`

**修改**
- `SolarisCompat.java`(瘦身)、`build.gradle`(主,加依赖声明不需要——主不依赖 RPG 代码;仅 `mods.toml`)、`src/main/resources/META-INF/mods.toml`、主 `lang/{zh_cn,en_us}.json`、`.gitignore`
- RPG 代码内解耦:`RpgNetwork`、`SFactory`、`DialogueRoot`、`ScriptRegistry`、`kjs.SolarisRPGKubeJSPlugin`

---

### Task 1: 搭建 `:solaris_rpg` 子工程骨架 + 更新 `.gitignore`

**Files:**
- Modify: `settings.gradle`、`.gitignore`
- Create: `solaris_rpg/build.gradle`、`solaris_rpg/src/main/resources/META-INF/mods.toml`、`solaris_rpg/src/main/resources/pack.mcmeta`、`solaris_rpg/src/main/java/dev/efm/rpg/SolarisRpg.java`

**Interfaces:**
- Produces: `dev.efm.rpg.SolarisRpg.MODID`(值 `"solaris_rpg"`),供后续所有任务引用。

- [ ] **Step 1: 在 `settings.gradle` 末尾追加 include**

在 `rootProject.name = 'solaris_compat'` 之后加一行:

```groovy
include 'solaris_rpg'
```

- [ ] **Step 2: 更新 `.gitignore`**

在现有 6 行之后追加:

```
# 子工程生成目录
/solaris_rpg/.gradle/
/solaris_rpg/build/
/solaris_rpg/run/
/solaris_rpg/run-data/

# 编辑器 / 系统
/.vscode/
**/.DS_Store
Thumbs.db
```

- [ ] **Step 3: 创建 `solaris_rpg/build.gradle`**

```groovy
plugins {
    id 'eclipse'
    id 'idea'
    id 'net.minecraftforge.gradle' version '[6.0.16,6.2)'
    id 'org.parchmentmc.librarian.forgegradle' version '1.+'
}

group = mod_group_id
version = mod_version

base {
    archivesName = 'solaris_rpg'
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(17)
}

minecraft {
    mappings channel: mapping_channel, version: mapping_version
    copyIdeResources = true

    runs {
        configureEach {
            workingDirectory project.file('run')
            property 'forge.logging.markers', 'REGISTRIES'
            property 'forge.logging.console.level', 'debug'
            mods {
                'solaris_rpg' {
                    source sourceSets.main
                }
            }
        }
        client {
            property 'forge.enabledGameTestNamespaces', 'solaris_rpg'
            jvmArgs("-Xmx6G")
        }
        server {
            property 'forge.enabledGameTestNamespaces', 'solaris_rpg'
            args '--nogui'
        }
    }
}

repositories {
    flatDir { dir rootProject.file('libs') }
    maven {
        url = "https://maven.architectury.dev"
        content { includeGroup "dev.architectury" }
    }
    maven { url 'https://modmaven.dev/' }
    mavenCentral()
}

dependencies {
    minecraft "net.minecraftforge:forge:${minecraft_version}-${forge_version}"

    implementation fg.deobf("libs:ldlib-forge-1.20.1:1.0.52.a")
    implementation fg.deobf("dev.architectury:architectury-forge:${architectury_version}")

    compileOnly fg.deobf("libs:kubejs-forge:${kubejs_version}")
    compileOnly fg.deobf("libs:rhino-forge:${rhino_version}")
    runtimeOnly fg.deobf("libs:kubejs-forge:${kubejs_version}")
    runtimeOnly fg.deobf("libs:rhino-forge:${rhino_version}")
}

tasks.named('processResources', ProcessResources).configure {
    var replaceProperties = [minecraft_version   : minecraft_version, minecraft_version_range: minecraft_version_range,
                             forge_version       : forge_version, forge_version_range: forge_version_range,
                             loader_version_range: loader_version_range,
                             mod_license         : mod_license, mod_version: mod_version, mod_authors: mod_authors]
    inputs.properties replaceProperties
    filesMatching(['META-INF/mods.toml', 'pack.mcmeta']) {
        expand replaceProperties + [project: project]
    }
}

tasks.named('jar', Jar).configure {
    manifest {
        attributes(["Specification-Title"     : 'solaris_rpg',
                    "Specification-Vendor"    : mod_authors,
                    "Specification-Version"   : "1",
                    "Implementation-Title"    : project.name,
                    "Implementation-Version"  : project.jar.archiveVersion,
                    "Implementation-Timestamp": new Date().format("yyyy-MM-dd'T'HH:mm:ssZ")])
    }
    finalizedBy 'reobfJar'
}

tasks.withType(JavaCompile).configureEach {
    options.encoding = 'UTF-8'
}
```

- [ ] **Step 4: 创建 `solaris_rpg/src/main/resources/META-INF/mods.toml`**

```toml
modLoader = "javafml"
loaderVersion = "${loader_version_range}"
license = "${mod_license}"

[[mods]]
modId = "solaris_rpg"
version = "${mod_version}"
displayName = "Solaris RPG"
authors = "${mod_authors}"
description = '''Solaris RPG dialogue framework'''

[[dependencies.solaris_rpg]]
modId = "forge"
mandatory = true
versionRange = "${forge_version_range}"
ordering = "NONE"
side = "BOTH"

[[dependencies.solaris_rpg]]
modId = "minecraft"
mandatory = true
versionRange = "${minecraft_version_range}"
ordering = "NONE"
side = "BOTH"

[[dependencies.solaris_rpg]]
modId = "ldlib"
mandatory = true
versionRange = "[0,10000000)"
ordering = "NONE"
side = "BOTH"

[[dependencies.solaris_rpg]]
modId = "kubejs"
mandatory = false
versionRange = "[0,)"
ordering = "AFTER"
side = "BOTH"
```

- [ ] **Step 5: 创建 `solaris_rpg/src/main/resources/pack.mcmeta`**

```json
{
  "pack": {
    "description": "solaris_rpg resources",
    "pack_format": 15
  }
}
```

- [ ] **Step 6: 创建最小 `SolarisRpg` 入口**(本任务只求能加载,接线留到 Task 2)

```java
package dev.efm.rpg;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SolarisRpg.MODID)
public class SolarisRpg {
    public static final String MODID = "solaris_rpg";

    public SolarisRpg(FMLJavaModLoadingContext context) {
    }
}
```

- [ ] **Step 7: 构建两工程**

Run: `gradlew :solaris_rpg:build` 然后 `gradlew build`
Expected: 均 BUILD SUCCESSFUL;`solaris_rpg/build/libs/solaris_rpg-1.1.2.a.jar` 存在。

- [ ] **Step 8: 核对子工程 jar 内容**

Run: `jar --list --file solaris_rpg/build/libs/solaris_rpg-1.1.2.a.jar`
Expected: 含 `META-INF/mods.toml`、`pack.mcmeta`、`dev/efm/rpg/SolarisRpg.class`;`mods.toml` 内无未展开的 `${...}`(可 `jar --extract` 后查看)。

- [ ] **Step 9: 提交**

```bash
git add settings.gradle .gitignore solaris_rpg/build.gradle solaris_rpg/src
git commit -m "build: 新增 solaris_rpg 子工程骨架与 .gitignore 忽略项"
```

---

### Task 2: 迁移 RPG 核心 + KubeJS 层,建立自包含入口,断开主 mod 引用,迁移核心资源

> 本任务必须原子完成:主 mod 的 `kjs` 包引用 `dev.efm.rpg`,而 `dev.efm.rpg` 一旦迁出主 mod,主 mod 立即编译失败。因此核心、KJS 层、入口接线、主 mod 引用清理、核心资源必须同一次完成。

**Files:**
- Move: `src/main/java/dev/efm/rpg/**` → `solaris_rpg/src/main/java/dev/efm/rpg/**`
- Move: `src/main/java/dev/efm/solaris_compat/kjs/**` → `solaris_rpg/src/main/java/dev/efm/rpg/kjs/**`
- Move: `src/main/resources/kubejs.plugins.txt` → `solaris_rpg/src/main/resources/`
- Move: `src/main/resources/assets/solaris_compat/textures/gui/sola_background.png` → `solaris_rpg/src/main/resources/assets/solaris_rpg/textures/gui/sola_background.png`
- Move: `src/main/resources/data/solaris_compat/solaris_rpg/demo.json` → `solaris_rpg/src/main/resources/data/solaris_rpg/solaris_rpg/demo.json`
- Modify: `dev/efm/rpg/SolarisRpg.java`、`dev/efm/rpg/network/RpgNetwork.java`、`dev/efm/rpg/SFactory.java`、`dev/efm/rpg/widgets/DialogueRoot.java`、`dev/efm/rpg/data/ScriptRegistry.java`、`dev/efm/rpg/kjs/SolarisRPGKubeJSPlugin.java`、`SolarisCompat.java`、`src/main/resources/META-INF/mods.toml`

**Interfaces:**
- Consumes: `SolarisRpg.MODID`(Task 1)。
- Produces: RPG mod 完整自包含入口;主 mod 不再引用 `dev.efm.rpg`。

- [ ] **Step 1: 搬迁核心 Java 与 KJS 层**

```bash
git mv src/main/java/dev/efm/rpg solaris_rpg/src/main/java/dev/efm/rpg
git mv src/main/java/dev/efm/solaris_compat/kjs solaris_rpg/src/main/java/dev/efm/rpg/kjs
git mv src/main/resources/kubejs.plugins.txt solaris_rpg/src/main/resources/kubejs.plugins.txt
```

> `git mv` 目标父目录不存在时先建:`mkdir -p solaris_rpg/src/main/java/dev/efm`。

- [ ] **Step 2: 改 KJS 层包声明与引用**

在 `solaris_rpg/src/main/java/dev/efm/rpg/kjs/` 三个文件中:
- `package dev.efm.solaris_compat.kjs;` → `package dev.efm.rpg.kjs;`(三处)
- `SolarisRPGKubeJSPlugin` 中 `filter.allow("dev.efm.solaris_compat.kjs");` → `filter.allow("dev.efm.rpg.kjs");`
- 确认 `filter.allow("dev.efm.rpg");` 保留不变。
- `kubejs.plugins.txt` 内容类名改为 `dev.efm.rpg.kjs.SolarisRPGKubeJSPlugin`。

- [ ] **Step 3: 解耦 `RpgNetwork` 与 `SFactory`**

- `RpgNetwork`:删除 `import dev.efm.solaris_compat.SolarisCompat;` 与 `import dev.efm.solaris_compat.api.SHelper;`;新增 `import dev.efm.rpg.SolarisRpg;` 与 `import net.minecraft.resources.ResourceLocation;`;`SHelper.buildRes(SolarisCompat.MODID, "rpg")` → `ResourceLocation.fromNamespaceAndPath(SolarisRpg.MODID, "rpg")`。
- `SFactory`:同样删除对 `SolarisCompat`/`SHelper` 的 import;`super(SHelper.buildRes(SolarisCompat.MODID, "gui"))` → `super(ResourceLocation.fromNamespaceAndPath(SolarisRpg.MODID, "gui"))`,并补 `import net.minecraft.resources.ResourceLocation;`。

- [ ] **Step 4: 把接线搬进 `SolarisRpg`**

将 `SolarisCompat.java` 中下列三个方法体**原样**迁入 `SolarisRpg`(签名与方法体不变),并在构造器接线:

```java
@Mod(SolarisRpg.MODID)
public class SolarisRpg {
    public static final String MODID = "solaris_rpg";

    public SolarisRpg(FMLJavaModLoadingContext context) {
        var fbus = MinecraftForge.EVENT_BUS;
        var ibus = context.getModEventBus();
        ibus.addListener(this::commonSetup);
        fbus.addListener(this::onCommand);
        fbus.addListener(this::onAddReloadListeners);
        ScriptRegistry.defaultReg();
    }

    public void onAddReloadListeners(AddReloadListenerEvent event) { event.addListener(new ScriptReloadListener()); }

    public void onCommand(RegisterCommandsEvent evt) { /* 迁移 SolarisCompat.onCommand 全body: /std_create */ }

    public void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            UIFactory.register(SFactory.INSTANCE);
            RpgNetwork.register();
        });
    }
}
```

`onCommand` / `onAddReloadListeners` 的完整实现从 `SolarisCompat.java` 现有版本照搬,import 相应调整(`dev.efm.rpg.data.*`、`dev.efm.rpg.network.*`、`com.lowdragmc.lowdraglib.gui.factory.UIFactory`、brigadier、`Component`、`ServerPlayer` 等)。

- [ ] **Step 5: 瘦身主 mod `SolarisCompat.java`**

删除:全部 `dev.efm.rpg.*` import;`onCommand` 与 `onAddReloadListeners` 方法;`commonSetup` 方法及 `ibus.addListener(this::commonSetup)`;`fbus.addListener(this::onCommand)`、`fbus.addListener(this::onAddReloadListeners)`;`ScriptRegistry.defaultReg()` 调用;不再使用的 `var fbus = MinecraftForge.EVENT_BUS;`(若无其它 FORGE 监听)。

保留:`SolarisConfig` 注册、`ConfigScreen` 扩展点、`DataRegistry::DataRegistryEvent`/`GatherDataEvent`、`SRegistry.register(ibus)`、`randomListHundred` 初始化、清理无用 import。

- [ ] **Step 6: 迁移核心资源并改命名空间**

```bash
git mv src/main/resources/assets/solaris_compat/textures/gui/sola_background.png solaris_rpg/src/main/resources/assets/solaris_rpg/textures/gui/sola_background.png
git mv src/main/resources/data/solaris_compat/solaris_rpg solaris_rpg/src/main/resources/data/solaris_rpg
```

- `DialogueRoot`:背景 `"solaris_compat:textures/gui/sola_background.png"` → `"solaris_rpg:textures/gui/sola_background.png"`。
- `ScriptRegistry`:常量 `PORTRAIT_OLIVIA` → `"solaris_rpg:textures/gui/portrait/olivia.png"`;注释里的旧路径同步。
- `demo.json`:其 `portrait` 值 → `"solaris_rpg:textures/gui/portrait/olivia.png"`(文件缺失为已知项,见 spec 第 8 节)。

- [ ] **Step 7: 主 mod `mods.toml` 增加前置声明**

```toml
[[dependencies."${mod_id}"]]
modId = "solaris_rpg"
mandatory = true
versionRange = "[0,)"
ordering = "AFTER"
side = "BOTH"
```

- [ ] **Step 8: 静态检查(Review Focus 1、2、3)**

```bash
# 1) 主 mod 不得再引用 RPG 或旧 kjs 包
git grep -n "dev.efm.rpg" -- src/main/java || echo "OK: no rpg refs in main"
git grep -n "dev.efm.solaris_compat.kjs" -- src || echo "OK: no old kjs pkg"

# 2) RPG 代码不得残留 solaris_compat: 资源命名空间
git grep -n "solaris_compat:" -- solaris_rpg/src/main/java dev.efm 2>/dev/null || echo "OK: no legacy namespace"

# 3) 核心包不得硬引用 KubeJS
git grep -n "dev.latvian" -- solaris_rpg/src/main/java/dev/efm/rpg -- ':!*/kjs/*' || echo "OK: core has no kubejs import"
```

Expected: 三条均打印 OK 行。

- [ ] **Step 9: 构建**

Run: `gradlew :solaris_rpg:build` 然后 `gradlew build`
Expected: 均 BUILD SUCCESSFUL。

- [ ] **Step 10: 游戏内冒烟(人工观察)**

Run: `gradlew :solaris_rpg:runServer`(或 `runClient`)
Expected: mod `solaris_rpg` 加载无异常;`/std_create solaris_rpg:demo` 可打开界面;背景图显示;`/reload` 重载剧本日志 `从数据包加载了 1 个剧本`。

- [ ] **Step 11: 提交**

```bash
git add -A
git commit -m "refactor: RPG 核心与 KubeJS 层迁出为 solaris_rpg 子工程并自包含接线"
```

---

### Task 3: 迁移 Olivia 实体与渲染(含贴图与语言键)

**Files:**
- Move: `src/main/java/dev/efm/solaris_compat/common/entity/OliviaEntity.java`、`OliviaEntities.java` → `solaris_rpg/src/main/java/dev/efm/rpg/entity/`
- Move: `src/main/java/dev/efm/solaris_compat/client/OliviaRenderer.java`、`OliviaClientEvents.java` → `solaris_rpg/src/main/java/dev/efm/rpg/client/`
- Move: `src/main/resources/assets/solaris_compat/textures/entity/olivia.png` → `solaris_rpg/src/main/resources/assets/solaris_rpg/textures/entity/olivia.png`
- Create: `solaris_rpg/src/main/resources/assets/solaris_rpg/lang/zh_cn.json`、`en_us.json`
- Modify: `src/main/resources/assets/solaris_compat/lang/zh_cn.json`、`en_us.json`

**Interfaces:**
- Consumes: `SolarisRpg.MODID`(Task 1)。
- Produces: `dev.efm.rpg.entity.OliviaEntities.OLIVIA`、`dev.efm.rpg.client.OliviaRenderer`。

- [ ] **Step 1: 搬迁并改包/导入**

```bash
mkdir -p solaris_rpg/src/main/java/dev/efm/rpg/entity solaris_rpg/src/main/java/dev/efm/rpg/client
git mv src/main/java/dev/efm/solaris_compat/common/entity/OliviaEntity.java solaris_rpg/src/main/java/dev/efm/rpg/entity/OliviaEntity.java
git mv src/main/java/dev/efm/solaris_compat/common/entity/OliviaEntities.java solaris_rpg/src/main/java/dev/efm/rpg/entity/OliviaEntities.java
git mv src/main/java/dev/efm/solaris_compat/client/OliviaRenderer.java solaris_rpg/src/main/java/dev/efm/rpg/client/OliviaRenderer.java
git mv src/main/java/dev/efm/solaris_compat/client/OliviaClientEvents.java solaris_rpg/src/main/java/dev/efm/rpg/client/OliviaClientEvents.java
```

- `OliviaEntity`:包 → `dev.efm.rpg.entity`。
- `OliviaEntities`:包 → `dev.efm.rpg.entity`;`@Mod.EventBusSubscriber(modid = SolarisCompat.MODID, ...)` → `modid = SolarisRpg.MODID`;删 `import dev.efm.solaris_compat.SolarisCompat;`,加 `import dev.efm.rpg.SolarisRpg;`。
- `OliviaRenderer`:包 → `dev.efm.rpg.client`;`import dev.efm.solaris_compat.common.entity.OliviaEntity;` → `dev.efm.rpg.entity.OliviaEntity`;`SolarisCompat.MODID` → `SolarisRpg.MODID` + import。
- `OliviaClientEvents`:包 → `dev.efm.rpg.client`;`import ...common.entity.OliviaEntities;` → `dev.efm.rpg.entity.OliviaEntities`;`SolarisCompat.MODID` → `SolarisRpg.MODID` + import。

- [ ] **Step 2: 搬迁实体贴图**

```bash
git mv src/main/resources/assets/solaris_compat/textures/entity/olivia.png solaris_rpg/src/main/resources/assets/solaris_rpg/textures/entity/olivia.png
```

- [ ] **Step 3: 迁移语言键**

创建 `solaris_rpg/src/main/resources/assets/solaris_rpg/lang/zh_cn.json`:

```json
{
  "entity.solaris_rpg.olivia": "Olivia"
}
```

创建 `solaris_rpg/src/main/resources/assets/solaris_rpg/lang/en_us.json`:

```json
{
  "entity.solaris_rpg.olivia": "Olivia"
}
```

从 `src/main/resources/assets/solaris_compat/lang/zh_cn.json` 与 `en_us.json` 删除 `"entity.solaris_compat.olivia"` 行(注意保留前一行的逗号结构合法)。

- [ ] **Step 4: 静态检查(Review Focus 6)**

```bash
git grep -n "entity.solaris_compat.olivia" -- src || echo "OK: old key removed"
git grep -n "entity.solaris_rpg.olivia" -- solaris_rpg || echo "MISSING new key"
git grep -n "SolarisCompat" -- solaris_rpg/src || echo "OK: no main-mod refs in rpg"
```

Expected: 第一、三条打印 OK,第二条打印新键命中。

- [ ] **Step 5: 构建**

Run: `gradlew :solaris_rpg:build` 然后 `gradlew build`
Expected: BUILD SUCCESSFUL;两个主 lang 文件仍是合法 JSON。

- [ ] **Step 6: 游戏内冒烟(人工观察)**

Run: `gradlew :solaris_rpg:runClient`
Expected: `/summon solaris_rpg:olivia` 生成实体且贴图正常、名称显示 `Olivia`(非翻译键)。

- [ ] **Step 7: 提交**

```bash
git add -A
git commit -m "refactor: Olivia 实体与渲染迁入 solaris_rpg 子工程"
```

---

### Task 4: 全量验证与收尾

**Files:**
- 无新增;只做检查与必要的收尾修正。

**Interfaces:**
- Consumes: Task 1–3 的全部产物。

- [ ] **Step 1: 清理构建产物后全量构建**

```bash
gradlew clean
gradlew :solaris_rpg:build
gradlew build
```

Expected: 全部 BUILD SUCCESSFUL;两 jar 均存在于各自 `build/libs/`。

- [ ] **Step 2: 类冲突交叉比对(Review Focus 5)**

```bash
jar --list --file build/libs/solaris_compat-1.1.2.a.jar | findstr /R "\.class$" > main_classes.txt
jar --list --file solaris_rpg/build/libs/solaris_rpg-1.1.2.a.jar | findstr /R "\.class$" > rpg_classes.txt
```

Expected: 两文件无交集(可用 `sort` + 比较工具核对)。特别注意 `dev/efm/rpg/**` 只出现在 RPG jar。

- [ ] **Step 3: `mods.toml` 展开与依赖核对(Review Focus 4)**

用 `jar --extract` 取出两 jar 的 `META-INF/mods.toml`,确认:
- 无残留 `${...}`;
- 主 mod 含 `modId = "solaris_rpg"` 的必需依赖;
- RPG mod 的 `kubejs` 为 `mandatory = false`。

- [ ] **Step 4: 联装冒烟(人工观察)**

将两个 jar 一起放入 `run/mods`,启动游戏:
Expected: 两 mod 均加载;无 `NoClassDefFoundError`;RPG 对话与主 mod 兼容功能同场可用。

- [ ] **Step 5: 主 mod 功能回归(人工观察)**

Expected: 灌注器强化、流体抽屉产水、注魔机、悬赏池缓存均与拆分前一致(未装 RPG 时主 mod 仍可独立加载)。

- [ ] **Step 6: 最终提交(若有收尾改动)**

```bash
git add -A
git commit -m "chore: RPG 子工程拆分验证与收尾"
```

---

## Self-Review 记录

- **Spec coverage:** spec 第 1 节→Task 1;第 2/3/4/6 节→Task 2;第 5 节(mods.toml/资源)→Task 1(新工程)+Task 2(核心资源)+Task 3(实体资源);第 7 节(.gitignore)→Task 1;第 9 节(验证)→散布各任务 + Task 4;第 8 节(已知问题)明确不修,仅在 Task 2 Step 6 记录。无遗漏。
- **Step scan:** 每个 Step 单一动作、可判定;无 "TBD"/"handle edge cases" 式空话。
- **Type consistency:** `SolarisRpg.MODID`、`OliviaEntities.OLIVIA`、`dev.efm.rpg.kjs` 包名在任务间一致。
- **Review Focus:** 6 条风险均落到具体检查步骤。
- **Proportion:** 计划以路径映射与检查命令为主,未转写业务代码。
