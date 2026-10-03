// priority: 10000
// 唯一配置：kubejs/config/disabled_items.json。修改后完整重启。
// 此文件在客户端和服务端读取同一黑名单；联机时两边均须同步配置。
const solarisDisabledConfig = JSON.parse(JsonIO.toString(JsonIO.readJson('kubejs/config/disabled_items.json')))
if (!solarisDisabledConfig || !Array.isArray(solarisDisabledConfig.items)) {
    throw new Error('[Disabled Items] Config must contain an items array.')
}
const solarisDisabledIds = []
const solarisDisabledLookup = {}
solarisDisabledConfig.items.forEach(value => {
    if (typeof value !== 'string' || !/^[a-z0-9_.-]+:[a-z0-9_./-]+$/.test(value) || value === 'minecraft:air') {
        throw new Error('[Disabled Items] Invalid item ID: ' + value)
    }
    if (!solarisDisabledLookup[value]) {
        solarisDisabledLookup[value] = true
        solarisDisabledIds.push(value)
    }
})
global.SolarisDisabledItems = {
    ids: solarisDisabledIds,
    has: stack => !!stack && !stack.empty && !!solarisDisabledLookup[String(stack.id)]
}

// ForgeEvents 只在 startup_scripts 可用；在物品原生行为执行前取消。
const solarisPlayerClass = Java.loadClass('net.minecraft.world.entity.player.Player')
function solarisDenyStackEvent(event) {
    if (global.SolarisDisabledItems.has(event.getItemStack())) event.setCanceled(true)
}
ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickItem', solarisDenyStackEvent)
ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock', solarisDenyStackEvent)
ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerInteractEvent$EntityInteract', solarisDenyStackEvent)
ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerInteractEvent$EntityInteractSpecific', solarisDenyStackEvent)
ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerInteractEvent$LeftClickBlock', solarisDenyStackEvent)
ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingEntityUseItemEvent$Start', event => {
    if (event.getEntity() instanceof solarisPlayerClass && global.SolarisDisabledItems.has(event.getItem())) {
        event.setCanceled(true)
    }
})
ForgeEvents.onEvent('net.minecraftforge.event.entity.player.AttackEntityEvent', event => {
    if (global.SolarisDisabledItems.has(event.getEntity().getMainHandItem())) event.setCanceled(true)
})
ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerEvent$BreakSpeed', event => {
    if (global.SolarisDisabledItems.has(event.getEntity().getMainHandItem())) event.setCanceled(true)
})
console.info('[Disabled Items] Loaded ' + solarisDisabledIds.length + ' disabled item IDs.')
