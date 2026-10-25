# Solaris

整合包核心 mod，Gradle 多项目结构：

| 子项目 | modid | 职责 |
| --- | --- | --- |
| `solaris_core`（根项目） | `solaris_core` | 数据驱动能力：战利品替换（Global Loot Modifier）、结构替换 / 多方块结构加载、`StructureTemplateMixin` |
| `solaris_functions` | `solaris_functions` | 玩法功能：死亡点回溯、TPA、重生保护、血量上限修复、无经验冷却、始终可食用、禁绊线、怪物清理、核弹实体与客户端渲染 |

`solaris_functions` 在编译期依赖 `solaris_core` 的 named classes（`build/classes/java/main`），
运行期则作为独立 mod 依赖 `solaris_core`（见 `mods.toml` 的 `dependencies` 段）。

## 构建

```bash
# 全部（根项目 + 子项目）
gradle build

# 只构建 core（根项目）
gradle :build

# 只构建功能模块
gradle :solaris_functions:build
```

产物：

- `build/libs/solaris_core-<version>.jar`
- `solaris_functions/build/libs/solaris_functions-<version>.jar`

## 运行

```bash
gradle runClient                  # core 客户端
gradle :solaris_functions:runClient   # 功能模块客户端（classpath 上带 core）
```

`solaris_functions` 的 `runs.mods { solaris_core { ... } }` 会把 core 的 classes/资源
写进 `MOD_CLASSES`，FML 会把它当作独立 mod 加载，所以开发时无需先打包 core。

## 本地依赖

`libs/` 下的 cloth-config、ldlib、packetfixer 只被功能模块使用，
由 `solaris_functions` 的 `jarJar` 打包；core 不再携带这些库。
