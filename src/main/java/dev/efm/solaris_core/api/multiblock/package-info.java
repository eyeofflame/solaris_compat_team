/**
 * IE 式多方块机器 API（替换型）。
 *
 * <h2>生命周期</h2>
 * <ol>
 *     <li><b>检测前</b>：结构里每一格都是任意方块，可以全部来自别的 mod，包括 trigger（控制器）那一格。</li>
 *     <li><b>成型</b>：手持配置内的触发物品右键 trigger 方块 → 读 {@code .nbt} 模板 → 找一个能对准世界的旋转 →
 *         逐格精确比对 blockstate → 全部通过后整座替换成 {@link dev.efm.solaris_core.api.multiblock.SolarisPartBlock}。</li>
 *     <li><b>运行</b>：trigger 那一格是 master（{@link dev.efm.solaris_core.api.multiblock.SolarisMasterBlockEntity}），
 *         持有图纸 id、旋转和机器数据；其余格是 dummy，只负责转发能力查询。</li>
 *     <li><b>拆解</b>：破坏任意一格 → 按同一张 {@code .nbt} + master 里存的旋转还原成材料；被敲的那一格留空。</li>
 * </ol>
 *
 * <h2>接入步骤</h2>
 * <pre>{@code
 * // 1. 结构方块搭好材料（精确状态），存成 data/solaris/structures/coke_oven.nbt
 *
 * // 2. 注册图纸
 * public static final Multiblock COKE_OVEN = MultiblockRegistry.register(
 *         Multiblock.builder("solaris:coke_oven")
 *                 .template("solaris:coke_oven")
 *                 .triggerOffset(1, 0, 1)
 *                 .trigger(Blocks.BLAST_FURNACE)   // 别人的方块
 *                 .build());
 *
 * // 3. 注册机器行为
 * MultiblockLogics.register(COKE_OVEN.getId(), new IMultiblockLogic() {
 *     @Override public void tick(ServerLevel level, SolarisMasterBlockEntity master) { ... }
 *     @Override public void onBroken(ServerLevel level, SolarisMasterBlockEntity master) { ... }
 * });
 *
 * // 4. config/solaris_core-common.toml
 * //    [multiblock]
 * //    trigger_items = ["immersiveengineering:hammer"]
 * }</pre>
 *
 * <h2>边界（与 IE 10.2.0 对齐）</h2>
 * <ul>
 *     <li>精确 blockstate 匹配；材料朝向/属性不对会成型失败（旋转会先按点击面、再自动试其余 3 向）</li>
 *     <li>不做快照：还原用的是模板状态，不是玩家摆放时的精确状态</li>
 *     <li>材料里的 BlockEntity 数据在替换时丢失（IE 同样），所以模板里尽量不要放箱子/机器</li>
 *     <li>master 被爆炸或 /setblock 移除后无法还原（没有快照兜底）</li>
 *     <li>不做镜像（Mirror），需要时再扩展</li>
 * </ul>
 */
package dev.efm.solaris_core.api.multiblock;
