"""Validate the shipped OEI/OEF rules against the current ProbeJS snapshot."""
import json
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[1]


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def load_rules(namespace, kind):
    mapping = {}
    for path in sorted((ROOT / "data" / namespace / "replacements").glob("*.json")):
        for rule in read_json(path):
            assert set(rule) == {"match" + kind, "result" + kind}, str(path)
            target = rule["result" + kind]
            assert isinstance(target, str), str(path)
            assert rule["match" + kind], str(path)
            for source in rule["match" + kind]:
                assert source not in mapping, "Duplicate source: " + source
                mapping[source] = target
    return mapping


class UnificationTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.items = load_rules("oei", "Items")
        cls.fluids = load_rules("oef", "Fluids")
        cls.item_ids = {row["id"] for row in read_json(ROOT / ".vscode/item-attributes.json")}
        cls.fluid_rows = {row["id"]: row for row in read_json(ROOT / ".vscode/fluid-attributes.json")}
        cls.item_ids.update(row["bucketItem"] for row in cls.fluid_rows.values()
                            if row["hasBucket"] and row["bucketItem"] != "minecraft:air")
        cls.item_ids.update(item for row in read_json(ROOT / ".vscode/item-tag-attributes.json")
                            for item in row["items"])

    def test_all_ids_exist_in_probejs(self):
        for mapping, ids in ((self.items, self.item_ids), (self.fluids, self.fluid_rows)):
            for source, target in mapping.items():
                with self.subTest(source=source):
                    self.assertTrue(source in ids, "Unknown source: " + source)
                    self.assertTrue(target in ids, "Unknown target: " + target)

    def test_no_replacement_chains_or_self_replacements(self):
        for mapping in (self.items, self.fluids):
            self.assertFalse(set(mapping) & set(mapping.values()))

    def test_gtceu_material_priority(self):
        for source, target in {
            "thermal:iron_dust": "gtceu:iron_dust",
            "create:iron_sheet": "gtceu:iron_plate",
            "thermal:tin_ingot": "gtceu:tin_ingot",
            "mekanism:ingot_steel": "gtceu:steel_ingot",
            "create:brass_ingot": "gtceu:brass_ingot",
            "thermal:quartz_dust": "gtceu:nether_quartz_dust",
        }.items():
            with self.subTest(source=source):
                self.assertEqual(self.items.get(source), target)

    def test_metal_blocks_and_raw_blocks(self):
        for source, target in {
            "thermal:tin_block": "gtceu:tin_block",
            "mekanism:block_steel": "gtceu:steel_block",
            "create:brass_block": "gtceu:brass_block",
            "thermal:raw_lead_block": "gtceu:raw_lead_block",
            "eidolon:silver_block": "gtceu:silver_block",
        }.items():
            with self.subTest(source=source):
                self.assertEqual(self.items.get(source), target)

    def test_vanilla_materials_are_not_replaced(self):
        self.assertFalse(any(source.startswith("minecraft:") for source in self.items))

    def test_thermal_fallback(self):
        self.assertEqual(self.items.get("hammerlib:gears/gold"), "thermal:gold_gear")

    def test_safe_gems_use_gtceu_without_merging_charged_crystals(self):
        for source, target in {
            "thermal:ruby": "gtceu:ruby_gem",
            "thermal:sapphire": "gtceu:sapphire_gem",
            "thermal:apatite": "gtceu:apatite_gem",
            "thermal:cinnabar": "gtceu:cinnabar_gem",
        }.items():
            with self.subTest(source=source):
                self.assertEqual(self.items.get(source), target)
        self.assertNotIn("ae2:charged_certus_quartz_crystal", self.items)

    def test_preserve_processing_stages_and_special_materials(self):
        for source in ("create:crushed_raw_iron", "create:crushed_raw_tin",
                       "create:sturdy_sheet", "gtceu:treated_wood_rod",
                       "alexscaves:block_of_scarlet_neodymium",
                       "alexscaves:block_of_azure_neodymium",
                       "gtceu:high_octane_gasoline", "gtceu:cetane_boosted_diesel",
                       "gtceu:oil_light", "gtceu:oil_heavy", "gtceu:oil_medium"):
            self.assertNotIn(source, self.items)
            self.assertNotIn(source, self.fluids)

    def test_fuel_and_honey_targets(self):
        for source, target in {
            "gtceu:diesel": "createdieselgenerators:diesel",
            "gtceu:flowing_gasoline": "createdieselgenerators:flowing_gasoline",
            "gtceu:seed_oil": "createdieselgenerators:plant_oil",
            "thermal:crude_oil": "createdieselgenerators:crude_oil",
            "brewinandchewin:honey": "create:honey",
            "the_bumblezone:honey_fluid_flowing": "create:flowing_honey",
        }.items():
            with self.subTest(source=source):
                self.assertEqual(self.fluids.get(source), target)

    def test_fluid_buckets_agree_with_fluid_replacements(self):
        for source, target in self.fluids.items():
            source_bucket = self.fluid_rows[source]["bucketItem"]
            target_bucket = self.fluid_rows[target]["bucketItem"]
            if source_bucket != "minecraft:air" and source_bucket != target_bucket:
                with self.subTest(source=source):
                    self.assertEqual(self.items.get(source_bucket), target_bucket)

    def test_ore_host_stone_is_preserved(self):
        self.assertEqual(self.items.get("mekanism:lead_ore"), "gtceu:lead_ore")
        self.assertEqual(self.items.get("eidolon:deep_lead_ore"), "gtceu:deepslate_lead_ore")
        self.assertNotIn("gtceu:netherrack_lead_ore", self.items)


if __name__ == "__main__":
    unittest.main()
