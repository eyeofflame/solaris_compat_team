# Solaris

整合包核心 mod，Gradle 多项目结构：

| 子项目 | modid | 职责 |
| --- | --- | --- |
| `solaris_core`（根项目） | `solaris_core` | 数据驱动能力：战利品替换（Global Loot Modifier）、结构替换 / 多方块结构加载、多方块机器 API、`StructureTemplateMixin` |
| `solaris_functions` | `solaris_functions` | 玩法功能：死亡点回溯、TPA、重生保护、血量上限修复、无经验冷却、始终可食用、禁绊线、怪物清理、核弹实体与客户端渲染 |

`solaris_functions` 在编译期依赖 `solaris_core` 的 named classes（`build/classes/java/main`），
运行期则作为独立 mod 依赖 `solaris_core`（见 `mods.toml` 的 `dependencies` 段）。

---

## 多方块机器 API

`dev.efm.solaris_core.api.multiblock`：IE 式"整座替换"多方块。

- **检测前**：结构由任意方块搭成（可以全部来自别的 mod，包括 trigger 那一格）
- **成型**：手持配置物品右键 trigger 方块 → 读 `.nbt` 模板逐格精确比对 → 整座替换成 `solaris_core:part`
- **运行**：trigger 那一格是 master，持有图纸 id / 旋转 / 尺寸 / 机器数据；其余格是 dummy，只转发能力查询
- **拆解**：破坏任意一格 → 按同一张 `.nbt` + 存的旋转还原成材料，被敲格留空

方块与 BlockEntity 已由 core 注册，**你不需要注册任何方块**：

| 注册名 | 说明 |
| --- | --- |
| `solaris_core:part` | 部件方块，`slave` 属性区分 master / slave |
| `solaris_core:part_master` | master BE，持有图纸 id、旋转、尺寸、部件偏移、机器数据 |
| `solaris_core:part_dummy` | slave BE，只存自己相对 master 的偏移，能力查询转发给 master |

### 1. 准备结构模板

1. 用结构方块把材料按最终形状搭好——成型时逐格精确比对 blockstate，**朝向/属性必须一致**
2. 保存到 `data/<你的命名空间>/structures/<名字>.nbt`
3. 记下 **master（trigger）在模板里的坐标**（模板最小角是 `(0,0,0)`）
4. 模板里不要放带 BlockEntity 的方块（箱子、别的机器）——替换时它们的数据会丢

### 2. 注册图纸

在 common 初始化（mod 构造器或 `FMLCommonSetupEvent`）里注册；客户端也要注册（触发预检、渲染包围盒都要用）：

```java
public static final Multiblock COKE_OVEN = MultiblockRegistry.register(
        Multiblock.builder("mymod:coke_oven")        // 图纸 id
                .template("mymod:coke_oven")          // data/mymod/structures/coke_oven.nbt
                .triggerOffset(1, 0, 1)               // master 在模板里的坐标
                .trigger(Blocks.BLAST_FURNACE)        // 右键哪个方块触发（别的 mod 的方块也行）
                .build());
```

- `triggerOffset` 那一格在模板里应当就是 `trigger` 方块，成型后它变成 master
- `trigger` 只做"是不是控制器"的便宜预检，真正的校验是整张 `.nbt` 的精确比对
- 成型时先按点击面 / 玩家朝向决定旋转，失败再自动尝试其余 3 个旋转

### 3. 注册机器逻辑

按图纸 id 注册一次，回调全部在服务端：

```java
MultiblockLogics.register(COKE_OVEN.getId(), new IMultiblockLogic() {
    @Override
    public void onFormed(ServerLevel level, SolarisMasterBlockEntity master) {
        // 成型后：初始化库存、播声音、切方块状态……
    }

    @Override
    public void onBroken(ServerLevel level, SolarisMasterBlockEntity master) {
        // 拆解前（机器还在）：把库存掉出来
    }

    @Override
    public void tick(ServerLevel level, SolarisMasterBlockEntity master) {
        // 每个 tick；只有 master 有 ticker
    }
});
```

没注册逻辑的图纸也能成型，只是没有行为。

### 4. 配置触发物品

`config/solaris_core-common.toml`（或游戏内 Cloth Config 界面）：

```toml
[multiblock]
trigger_items = ["immersiveengineering:hammer", "minecraft:stick"]
```

- 元素是物品的 `ResourceLocation`，可以多个
- 客户端也会读这份列表来取消本地右键预测，两端配置要保持一致
- Cloth Config 界面：模组列表 → Solaris Core → Config → Multiblock → Trigger Items（点 `+` 添加）

### 5. 整机渲染（GeckoLib，可选）

部件方块是 `INVISIBLE`，整机由 master 的 GeckoLib 渲染。没注册模型的机器会走兜底（按部件偏移逐格画 `part` 方块），所以这步可以后补。

> GeckoLib 4.8.4 是 core 的必需依赖（`mods.toml` 里声明了 `geckolib`），客户端和服务端都要装。

#### 5.1 注册

```java
// 客户端，FMLClientSetupEvent 或更早
MultiblockModels.register(COKE_OVEN.getId(), new MultiblockModels.MultiblockModel(
        new ResourceLocation("mymod", "geo/multiblock/coke_oven.geo.json"),
        new ResourceLocation("mymod", "textures/multiblock/coke_oven.png"),
        new ResourceLocation("mymod", "animations/multiblock/coke_oven.animation.json"))); // 没动画传 null
```

资源文件：

| 类型 | 路径 |
| --- | --- |
| 模型 | `assets/mymod/geo/multiblock/coke_oven.geo.json` |
| 贴图 | `assets/mymod/textures/multiblock/coke_oven.png` |
| 动画（可选） | `assets/mymod/animations/multiblock/coke_oven.animation.json` |

#### 5.2 用 Blockbench 做模型

1. 安装 Blockbench 的 **GeckoLib Models** 插件
2. 新建 GeckoLib 模型，导入贴图
3. **以 master 方块为中心**来搭（1 格 = 16 单位，可以超出单格；GeckoLib 没有 ±1 格限制）
4. 导出 `.geo.json`（模型）和 `.animation.json`（动画，可选）到上表路径

坐标规则：

- 原点 = master 方块的最小角，1 格 = 16 单位
- 5×5×5、7×7×7 都能做，没有原版 JSON 的 `-16..32` 限制
- 模型的"正面"按模板默认朝向（北）搭；成型后渲染器会按存下来的 rotation 绕 master 中心旋转

#### 5.3 动画

在 `IMultiblockLogic` 里注册控制器：

```java
@Override
public void registerControllers(SolarisMasterBlockEntity master, AnimatableManager.ControllerRegistrar controllers) {
    controllers.add(new AnimationController<>(master, "main", 0, state -> PlayState.CONTINUE));
}
```

#### 5.4 先用示例模型验证链路

仓库里带了一个 3×3×3 实心壳的 GeckoLib 示例：`assets/solaris_core/geo/multiblock/example.geo.json`。

```java
MultiblockModels.register(COKE_OVEN.getId(), new MultiblockModels.MultiblockModel(
        new ResourceLocation("solaris_core", "geo/multiblock/example.geo.json"),
        new ResourceLocation("minecraft", "textures/block/iron_block.png"),
        null));
```

成型后看到 3×3×3 的铁块，就说明 GeckoLib 渲染、旋转、包围盒都通了。

### 6. 机器面板（可选）

右键机器任意一格时调用 `onInteract`（服务端，void）。打开面板、播声音、发消息都在这里自己处理，框架不再自动开界面：

```java
@Override
public void onInteract(ServerPlayer player, SolarisMasterBlockEntity master) {
    NetworkHooks.openScreen(player, new SimpleMenuProvider(
            (id, inv, p) -> new CokeOvenMenu(id, inv, master.getBlockPos()),
            Component.translatable("container.mymod.coke_oven")),
            buffer -> buffer.writeBlockPos(master.getBlockPos()));
}
```

你的 `MenuType` 建议用 `IContainerFactory`，按同样顺序 `buf.readBlockPos()` 拿到锚点。
玩家右键结构**任意一格**都会走到这里（dummy 会转发给 master）。
`Shift + 触发物品` 右键控制器是「整座复原」，不会触发 `onInteract`。

### 7. 数据与能力

```java
// 持久化数据：随 master 的 NBT 存档
CompoundTag data = master.getData();
data.putInt("progress", 42);
master.markDirty();
```

```java
// 按"结构内相对位置 + 面"提供能力（dummy 的查询会转发到这里）
@Override
public <T> LazyOptional<T> getCapabilityAt(SolarisMasterBlockEntity master, BlockPos posInMB,
                                           Capability<T> cap, Direction side) {
    if (cap == ForgeCapabilities.ITEM_HANDLER && posInMB.equals(new BlockPos(0, 1, 0))) {
        return LazyOptional.of(() -> myHandler);
    }
    return LazyOptional.empty();   // 不接管 → 退回 master 自己的（全局）能力
}
```

整机级能力也可以继续用 `AttachCapabilitiesEvent<BlockEntity>` 挂在 master 上。

### 8. 最小完整示例

```java
public final class CokeOven {
    public static final Multiblock MULTIBLOCK = MultiblockRegistry.register(
            Multiblock.builder("mymod:coke_oven")
                    .template("mymod:coke_oven")
                    .triggerOffset(1, 0, 1)
                    .trigger(Blocks.BLAST_FURNACE)
                    .build());

    public static void init() {
        MultiblockLogics.register(MULTIBLOCK.getId(), new IMultiblockLogic() {
            @Override
            public void onBroken(ServerLevel level, SolarisMasterBlockEntity master) {
                // 掉库存
            }

            @Override
            public void tick(ServerLevel level, SolarisMasterBlockEntity master) {
                // 机器逻辑
            }
        });
    }
}
```

在 mod 构造器里调用 `CokeOven.init()`；客户端再调 `MultiblockModels.register(...)` 配模型。

### 9. 边界

- **精确匹配**：模板每一格的 blockstate（含朝向）都要和世界一致，否则成型失败
- **不做镜像**，旋转会自动尝试 4 个方向
- **无快照**：拆解还原的是模板状态，不是玩家摆放时的精确状态
- **材料里的 BlockEntity 数据会丢**：模板别放箱子 / 机器
- **master 被爆炸或 `/setblock` 移走**后无法还原（没有快照兜底）
- 结构必须整体位于已加载区块内才能成型
- **整座复原**：`Shift + 触发物品` 右键**控制器（master）** → 按模板整座还原（包括控制器那一格）
- 直接挖部件方块也会拆解（`onRemove` 路径），但被敲的那一格留空
- `part` 方块本身不掉落；机器内容请在 `onBroken` 里自行掉落

---

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
