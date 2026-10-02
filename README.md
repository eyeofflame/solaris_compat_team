# Solaris Resources
用魔法生产资源！

## 原子转换配置

首次启动时会为每种原子生成 `config/solaris_resources/<path>.json`。
稳定原子与同类型的不稳定原子共用配置。例如 `iron.json`：

```json
{
  "convertedItem": "minecraft:raw_iron",
  "ratio": 8
}
```

`convertedItem` 为聚合后产出的物品 ID，`ratio` 为生成一个该物品所需的原子数。
物品 ID 必须存在，比例必须为非负整数；空物品 ID 或比例为 0 时不参与聚合。

编辑配置后执行 `/solaris_resources_reload` 即可立即生效，无需重启。
该指令需要 2 级权限，也可在服务端控制台执行。
已有背包或掉落中的原子会在下一次聚合时使用新规则。
失败项保留之前的有效配置（首次加载失败使用代码默认值），错误详情记录在日志中。
缺失的文件会重新生成默认配置；已有文件不会被覆盖。

开发验证：`gradle testAtomConfig test build`。

## 禁止自然矿石

新生成区块中的原版矿石、`forge:ores` 标签中的模组矿石都会被阻止生成；
普通地层（包括花岗岩、闪长岩、安山岩、凝灰岩和矿脉填充岩）会保留。
大型铁／铜矿脉中的矿石和粗矿块也会被过滤，但普通岩石不会变成空气。

可在 `config/solaris_resources/worldgen.json` 中补充未加入 `forge:ores` 的模组方块：

```json
{
  "additionalOreBlocks": [
    "examplemod:tin_ore"
  ]
}
```

配置文件缺失时自动生成；修改后执行 `/solaris_resources_reload`，后续生成检查立即使用新名单。
配置项必须是已注册方块 ID 的字符串数组。错误文件保留上一次有效配置并写入日志。

此功能只影响新区块生成，不清理旧区块，也不阻止玩家正常放置矿石或运行时使用 Feature。
未加入标签且未写入补充名单的自定义生成器，以及绕过受支持世界生成入口的特殊生成器，
需要单独适配。

独立验证命令：`gradle testWorldgenConfig testOrePolicy testAtomConfig test build runGameTestServer`。
