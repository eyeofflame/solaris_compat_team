# Solaris 物品与流体统一

## 规则入口

- `data/oei/replacements/`：物品替换，按材料分文件。
- `data/oei/replacements/metal_blocks.json`：金属块及粗金属块替换。
- `data/oei/replacements/buckets.json`：与流体统一同步的桶替换。
- `data/oef/replacements/`：静止态与流动态分别配置。

物品优先级为原版已有形态 → GTCEu → Thermal → Thermal Extra。原版没有的
形态（如铁粉、铜板）仍按 GTCEu 优先处理。没有上述目标模组的分组保持原样。
此外统一红宝石、蓝宝石、磷灰石和朱砂的普通宝石形态；不合并 AE2 充能晶体。

流体按明确的等价组统一：原油、柴油、汽油、植物油、生物柴油、乙醇优先
Create Diesel Generators，蜂蜜优先 Create。GTCEu 的轻油、重油、中质原油、
粗汽油、高辛烷值汽油和十六烷值提升柴油不合并。

保留粉碎矿与粗矿的加工阶段差异、不同矿石母岩、磁性钕块、处理木杆、
Create 坚固板等特殊组件。矿石仅统一普通石头与深板岩的对应形态。

## ProbeJS 数据依据

读取以下当前快照，不安装额外 Python 依赖：

- `.vscode/item-attributes.json`
- `.vscode/item-tag-attributes.json`
- `.vscode/fluid-attributes.json`

OEI 已生效时，物品导出列表可能不再包含被替换的源物品。因此标签成员和
流体 `bucketItem` 也作为源 ID 的证据；没有任何导出证据的旧 ID 不继续保留。
这些数据是导出时的快照，不代表修改后游戏的实时注册表；增删模组后应重新导出。

## 校验与维护

在 `kubejs/` 目录运行：

```powershell
python -m unittest discover -s tools -p "test_*.py" -v
python tools/unification.py
```

第一条检查实际交付的 JSON，包括 ID 依据、优先级、特殊材料保护、替换链、
重复源和桶一致性。第二条检查规则是否与当前导出数据及生成策略一致。

快照更新后可生成待审阅补丁（不会自动改动游戏数据）：

```powershell
python tools/unification.py --patch "C:/Users/Drifter/AppData/Local/Temp/opencode/solaris-unification.patch"
```

审阅补丁，尤其是新增标签成员的等价性，再应用；不要仅凭共享标签就认定
游戏行为相同。Python 脚本只是离线维护工具，不是游戏运行依赖。

## 游戏内验证

注意：上层 `.gitignore` 当前的 `oei`、`oef` 模式也会忽略这两个数据目录。
规则文件仍正常存在并供游戏加载，但普通 `git diff` 不会展示它们；若要提交，
需为 `kubejs/data/oei/`、`kubejs/data/oef/` 增加例外，或明确使用强制添加。

这些是 OEI/OEF 规则，而不是仅修改合成输出的 KubeJS 脚本。建议先备份存档，
完整重启客户端/服务器，并在测试存档检查 JEI、配方、材料互换、金属块拆合、
GTCEu/Create/Mekanism 燃料配方、桶与管道流体。离线测试不能证明模组内部
Java 注册对象引用或燃料识别已正确重定向，不能仅靠 `/reload` 判断全部生效。
