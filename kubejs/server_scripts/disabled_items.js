// priority: -10000
// 不注销物品注册，避免损坏模组引用；规则采用产出移除、使用拦截和持有清理。
const solarisBan = global.SolarisDisabledItems
if (!solarisBan) throw new Error('[Disabled Items] Startup blacklist was not loaded.')

ServerEvents.recipes(event => {
    solarisBan.ids.forEach(id => event.remove({ output: id }))
})
LootJS.modifiers(event => {
    if (!solarisBan.ids.length) return
    const modifier = event.addLootTableModifier(/.*/)
    solarisBan.ids.forEach(id => modifier.removeLoot(id))
    // 原版特殊掉落不经过普通战利品表，使用 LootJS 的专用拦截。
    if (solarisBan.ids.indexOf('minecraft:nether_star') !== -1) event.disableWitherStarDrop()
    if (solarisBan.ids.indexOf('minecraft:creeper_head') !== -1) event.disableCreeperHeadDrop()
    if (solarisBan.ids.indexOf('minecraft:skeleton_skull') !== -1) event.disableSkeletonHeadDrop()
    if (solarisBan.ids.indexOf('minecraft:zombie_head') !== -1) event.disableZombieHeadDrop()
})

// 正常玩家 inventory 包括主背包、快捷栏、护甲和副手。
// 使用 setter 而非只修改数量，确保库存容器能获知变更。
function solarisPurgeInventory(inventory) {
    if (!inventory) return false
    let changed = false
    for (let slot = 0; slot < inventory.slots; slot++) {
        if (solarisBan.has(inventory.getStackInSlot(slot))) {
            inventory.setStackInSlot(slot, Item.empty)
            changed = true
        }
    }
    return changed
}

const solarisCurios = Platform.isLoaded('curios')
    ? Java.loadClass('top.theillusivec4.curios.api.CuriosApi') : null
function solarisPurgePlayer(player) {
    if (!solarisBan.ids.length || player.level.clientSide) return
    let changed = solarisPurgeInventory(player.inventory)
    if (solarisPurgeInventory(player.enderChestInventory)) changed = true
    if (solarisPurgeInventory(player.craftingGrid)) changed = true
    if (solarisBan.has(player.mouseItem)) {
        player.mouseItem = Item.empty
        changed = true
    }
    if (solarisCurios) {
        solarisCurios.getCuriosInventory(player).ifPresent(handler => {
            handler.getCurios().values().forEach(slots => {
                // Curios setter 会触发其槽位更新流程。
                for (const inventory of [slots.getStacks(), slots.getCosmeticStacks()]) {
                    for (let slot = 0; slot < inventory.getSlots(); slot++) {
                        if (solarisBan.has(inventory.getStackInSlot(slot))) {
                            inventory.setStackInSlot(slot, Item.empty)
                            changed = true
                        }
                    }
                }
            })
        })
    }
    if (changed) player.sendInventoryUpdate()
}
PlayerEvents.loggedIn(event => solarisPurgePlayer(event.player))
PlayerEvents.respawned(event => solarisPurgePlayer(event.player))
PlayerEvents.tick(event => solarisPurgePlayer(event.player))
PlayerEvents.inventoryChanged(event => {
    if (solarisBan.has(event.item)) solarisPurgePlayer(event.player)
})
ItemEvents.canPickUp(event => {
    if (solarisBan.has(event.item)) event.cancel()
})
// 对绕过配方数据移除、仍产生结果的普通合成/烧炼事件进行兜底。
function solarisRejectCraft(event) {
    if (!solarisBan.has(event.item)) return
    event.item.count = 0
    solarisPurgePlayer(event.player)
}
ItemEvents.crafted(solarisRejectCraft)
ItemEvents.smelted(solarisRejectCraft)
