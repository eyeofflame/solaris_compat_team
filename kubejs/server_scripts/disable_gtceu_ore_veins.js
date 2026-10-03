// priority: -10000
// GTCEu 7.5.3：在服务端矿脉注册完成后移除普通矿脉与基岩矿脉。
// 全维度生效，只影响之后生成的区块；不删除矿石方块、物品或配方。
// 保留地下流体矿藏，不监听 fluidVeins。首次安装后请完整重启。
// 注意：config/gtceu.yaml 的 removeVanillaOreGen 当前为 true，
// 清除 GT 矿脉不会自动恢复原版矿石生成。

GTCEuServerEvents.oreVeins(event => {
    event.removeAll()
    console.info('[Solaris Worldgen] Removed all GTCEu ore vein definitions.')
})

GTCEuServerEvents.bedrockOreVeins(event => {
    const registries = Java.loadClass('com.gregtechceu.gtceu.api.registry.GTRegistries')
    // 本版本的基岩矿脉事件没有 removeAll；先复制键，避免边遍历边删除跳项。
    const ids = registries.BEDROCK_ORE_DEFINITIONS.keys().toArray()
    for (let i = 0; i < ids.length; i++) {
        event.remove(ids[i])
    }
    console.info('[Solaris Worldgen] Removed ' + ids.length + ' GTCEu bedrock ore vein definitions.')
})
