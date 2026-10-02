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
