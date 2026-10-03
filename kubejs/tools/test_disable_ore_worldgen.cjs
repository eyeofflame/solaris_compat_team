// Contract tests for script event registration, not a Minecraft runtime simulator.
const assert = require('node:assert/strict')
const fs = require('node:fs')
const path = require('node:path')
const vm = require('node:vm')
const root = path.resolve(__dirname, '..')

function run(relative, context) {
    const file = path.join(root, relative)
    assert.ok(fs.existsSync(file), `Missing worldgen script: ${relative}`)
    vm.runInNewContext(fs.readFileSync(file, 'utf8'), context, { filename: file })
}

const handlers = {}
const removed = []
const keys = ['gtceu:iron', 'gtceu:copper', 'addon:custom_ore']
const registry = { keys: () => ({ toArray: () => keys.slice() }) }
run('server_scripts/disable_gtceu_ore_veins.js', {
    console,
    Java: { loadClass(name) {
        assert.equal(name, 'com.gregtechceu.gtceu.api.registry.GTRegistries')
        return { BEDROCK_ORE_DEFINITIONS: registry }
    } },
    GTCEuServerEvents: {
        oreVeins: callback => { handlers.ore = callback },
        bedrockOreVeins: callback => { handlers.bedrock = callback }
    }
})
let cleared = 0
handlers.ore({ removeAll() { cleared++ } })
assert.equal(cleared, 1, 'GTCEu ordinary veins must be cleared')
handlers.bedrock({ remove(id) {
    removed.push(id)
    keys.splice(keys.indexOf(id), 1)
} })
assert.deepEqual(removed, ['gtceu:iron', 'gtceu:copper', 'addon:custom_ore'])
assert.equal(keys.length, 0, 'Removal must not skip entries when registry changes')

let startup
const featureIds = []
const predicates = []
run('startup_scripts/disable_mod_ore_worldgen.js', {
    console,
    WorldgenEvents: { remove: callback => { startup = callback } }
})
startup({
    removeFeatureById(step, ids) {
        assert.equal(step, 'underground_ores')
        featureIds.push(...ids)
    },
    removeOres(callback) {
        const properties = {}
        callback(properties)
        assert.equal(properties.worldgenLayer, 'underground_ores')
        predicates.push(properties.blocks)
    }
})
assert.deepEqual(featureIds.sort(), [
    'betterend:amber_ore', 'betterend:dragon_bone_ore',
    'betterend:ender_ore', 'betterend:thallasium_ore'
])
assert.equal(predicates.length, 1)
assert.ok(predicates[0].test('gtceu:deepslate_iron_ore'))
assert.ok(predicates[0].test('gtceu:netherrack_copper_ore'))
for (const id of ['minecraft:iron_ore', 'thermal:tin_ore', 'gtceu:rubber_log',
                  'betterend:umbrella_tree', 'betterend:flavolite']) {
    assert.equal(predicates[0].test(id), false, `Must preserve ${id}`)
}
console.log('Worldgen script contracts passed (veins, bedrock snapshot, ore-only features).')
