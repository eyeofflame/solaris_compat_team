// priority: -10000
// KubeJS 2001.6.5：世界生成移除事件必须在 startup_scripts 注册。
// 完整重启后对新区块生效；不会扫描或清除旧区块中的矿物。

WorldgenEvents.remove(event => {
    // GTCEu 的主矿脉由 server_scripts/disable_gtceu_ore_veins.js 清除。
    // 额外阻止使用普通 OreConfiguration 的 GT 矿石生成特征。
    event.removeOres(properties => {
        properties.worldgenLayer = 'underground_ores'
        properties.blocks = /^gtceu:.*_ore$/
    })

    // 来自 BetterEnd 20.0.11 的 placed_feature ID；不使用命名空间全删。
    // 龙骨是 minecraft:ore 类型的矿物沉积，只移除其地下矿石特征，
    // 保留 dragon_graveyards 群系、地表装饰和建筑。
    event.removeFeatureById('underground_ores', [
        'betterend:amber_ore',
        'betterend:dragon_bone_ore',
        'betterend:ender_ore',
        'betterend:thallasium_ore'
    ])
    console.info('[Solaris Worldgen] Registered GTCEu ore and BetterEnd ore feature removals.')
})
