# 矿石自然生成拦截：验证记录

实现分支：`feature/disable-natural-ores`，基线：`ef33adf`。

## 已验证

命令：`gradle testWorldgenConfig testOrePolicy testAtomConfig test build runGameTestServer --console=plain`

结果：BUILD SUCCESSFUL，9/9 必需 Forge GameTest 通过。

- 配置默认生成、合法读取、错误类型/未知 ID 拒绝、保留旧值、不可变并发快照。
- 原版完整矿石集合、模组标签、额外名单及普通地层判定。
- 普通矿脉和散布矿石实际放置流程：矿石禁止，混合目标中的普通岩石仍生成。
- WorldGenRegion：矿石写入失败并保留基底，普通岩石及非矿脉分支粗铁块放行。
- 普通 ServerLevel 写入及运行时 OreFeature 放行。
- 实际 OreVeinifier filler 在受控密度输入下不再产出矿石或粗矿块，填充岩保留。
- 测试数据包人工标记 Ars Nouveau 源质宝石块，验证标签识别与 Feature 拦截。
- 服务端资源重新加载：矿石标签移除和重新添加后判定即时更新，并恢复测试数据包。
- 重载指令权限检查、控制台来源执行、原子/世界生成配置失败隔离、两种原子共享更新。
- 完整命名空间校验回归：`:gold_block` 原先错误报告成功，新增测试先失败，修正后通过。
- 测试噪声主世界与下界区块检查：无矿石，普通地形保留。
- 产物包含 Mixin refmap；四个 Mixin 开发启动加载成功。

测试源和人工测试标签仅在 GameTest 运行中加载，不打包进发布 JAR。
日志：`build/ore-gametest/logs/latest.log`；构建产物：`build/libs/solaris_resources-1.0.0.jar`。

## 未验证及兼容边界

- 尚未做实际玩家客户端放置与 OP 指令操作；已验证 ServerLevel 写入、权限 0/2 和控制台来源。
- 尚未做旧矿石区块保存、关闭服务器再启动的验收；实现没有旧区块扫描或清理入口。
- 尚未在已知天然大型铁铜矿脉坐标进行基线对照。实际 filler 已验证；映射源码确认 null 结果由 NoiseBasedChunkGenerator 回退默认基底，而非空气。
- 尚未单独用结构模板验证结构装饰矿石；已验证结构使用的通用 WorldGenRegion 写入入口。
- 没有覆盖任意模组绕过所有已支持入口的直接区块写入生成器；这类生成器需要独立适配。

## 执行裁定与审查

- Ruling: POSIX 工作记录脚本改为 PowerShell/Markdown 记录。原因：环境没有可用 bash。代价：记录机制不同，不影响产品功能。
- Ruling: GameTestServer 在默认世界使用平坦地形，额外建立测试专用噪声主世界维度以检查真实地形生成。代价：未做真实主世界存档迁移验收；产品规则无维度过滤。
- Ruling: 上述未验证验收项明确披露，不将 9/9 GameTest 视为任意世界或任意模组的绝对覆盖保证。代价：部署到实际整合包前仍需对应游戏内验收。
- 独立审查未发现关键运行缺陷；指出验收覆盖不足、非规范 ID 验证及文档遗漏。已补标签重载、噪声主世界验证、规范 ID 回归和边界文档；剩余验收限制如上。
- Deferred minors: 无。ID 问题会导致用户配置成功却无效，按用户影响提升为必须修复并完成 RED→GREEN；文档边界作为交付要求补齐。
