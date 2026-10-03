const assert = require('node:assert/strict')
const fs = require('node:fs')
const vm = require('node:vm')
const path = require('node:path')
const root = path.resolve(__dirname, '..')
const stack = (id, count = 1) => ({ id, count, empty: false })
const banned = stack('minecraft:diamond')
const safe = stack('minecraft:apple')
const global = {}
const forge = {}
class Player {}
const context = {
    global, console,
    JsonIO: { readJson: () => ({ items: ['minecraft:diamond', 'minecraft:diamond', 'minecraft:nether_star',
        'minecraft:creeper_head', 'minecraft:skeleton_skull', 'minecraft:zombie_head'] }),
        toString: value => JSON.stringify(value) },
    Java: { loadClass: () => Player },
    ForgeEvents: { onEvent: (name, callback) => { forge[name] = callback } }
}
function run(file, ctx) {
    assert.ok(fs.existsSync(path.join(root, file)), 'Missing ' + file)
    vm.runInNewContext(fs.readFileSync(path.join(root, file), 'utf8'), ctx)
}
run('startup_scripts/disabled_items.js', context)
assert.equal(global.SolarisDisabledItems.ids.length, 5)
assert.equal(global.SolarisDisabledItems.has(banned), true)
assert.equal(global.SolarisDisabledItems.has(safe), false)
const player = new Player()
player.getMainHandItem = () => banned
let canceled = false
forge['net.minecraftforge.event.entity.player.AttackEntityEvent']({
    getEntity: () => player, setCanceled: value => { canceled = value }
})
assert.equal(canceled, true)
canceled = false
forge['net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickItem']({
    getItemStack: () => safe, setCanceled: value => { canceled = value }
})
assert.equal(canceled, false)
const handlers = {}
const recipeFilters = []
const lootRemoved = []
const register = name => callback => { handlers[name] = callback }
run('server_scripts/disabled_items.js', {
    global, console,
    Platform: { isLoaded: () => false },
    Item: { empty: { id: 'minecraft:air', empty: true, count: 0 } },
    ServerEvents: { recipes: register('recipes') },
    LootJS: { modifiers: register('loot') },
    PlayerEvents: { tick: register('tick'), loggedIn: register('login'),
        respawned: register('respawn'), inventoryChanged: register('inventory') },
    ItemEvents: { canPickUp: register('pickup'), crafted: register('crafted'), smelted: register('smelted') }
})
handlers.recipes({ remove: filter => recipeFilters.push(filter) })
assert.equal(recipeFilters[0].output, 'minecraft:diamond')
const specialDrops = []
handlers.loot({
    disableWitherStarDrop: () => specialDrops.push('star'),
    disableCreeperHeadDrop: () => specialDrops.push('creeper'),
    disableSkeletonHeadDrop: () => specialDrops.push('skeleton'),
    disableZombieHeadDrop: () => specialDrops.push('zombie'),
    addLootTableModifier: regex => {
    assert.ok(regex.test('minecraft:chests/test'))
    return { removeLoot: id => lootRemoved.push(id) }
} })
assert.deepEqual(lootRemoved, ['minecraft:diamond', 'minecraft:nether_star',
    'minecraft:creeper_head', 'minecraft:skeleton_skull', 'minecraft:zombie_head'])
assert.deepEqual(specialDrops, ['star', 'creeper', 'skeleton', 'zombie'])
const slots = [stack('minecraft:diamond'), safe, stack('minecraft:diamond')]
let updates = 0
const p = {
    level: { clientSide: false },
    inventory: { slots: slots.length, getStackInSlot: i => slots[i],
        setStackInSlot: (i, value) => { slots[i] = value } },
    mouseItem: stack('minecraft:diamond'),
    sendInventoryUpdate: () => { updates++ }
}
handlers.tick({ player: p })
assert.equal(slots[0].empty, true)
assert.equal(slots[1].id, 'minecraft:apple')
assert.equal(slots[2].empty, true)
assert.equal(p.mouseItem.empty, true)
assert.equal(updates, 1)
handlers.tick({ player: p })
assert.equal(updates, 1, 'Do not sync unchanged inventories')
let denied = false
handlers.pickup({ item: banned, cancel: () => { denied = true } })
assert.ok(denied)
const hidden = []
run('client_scripts/disabled_items.js', {
    global, JEIEvents: { hideItems: callback => callback({ hide: id => hidden.push(id) }) }
})
assert.deepEqual(hidden, ['minecraft:diamond', 'minecraft:nether_star',
    'minecraft:creeper_head', 'minecraft:skeleton_skull', 'minecraft:zombie_head'])
console.log('Disabled-item contracts passed: config, use, inventory, recipes, loot and JEI.')
