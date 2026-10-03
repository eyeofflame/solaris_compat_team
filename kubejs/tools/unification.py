"""Build OEI/OEF JSON from ProbeJS. Emit a patch; never edit game data directly."""
import argparse
from collections import defaultdict
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8-sig"))


def build_rules():
    items = {r["id"] for r in read_json(ROOT / ".vscode/item-attributes.json")}
    fluids = {r["id"]: r for r in read_json(ROOT / ".vscode/fluid-attributes.json")}
    # OEI can remove replaced buckets from the item dump; fluid metadata retains their IDs.
    items.update(r["bucketItem"] for r in fluids.values() if r["hasBucket"]
                 and r["bucketItem"] != "minecraft:air")
    tags = {r["id"]: r["items"] for r in read_json(ROOT / ".vscode/item-tag-attributes.json")}
    items.update(item for members in tags.values() for item in members)
    output = defaultdict(list)
    metals = {tag.removeprefix("forge:ingots/") for tag in tags
              if tag.startswith("forge:ingots/") and "/" not in tag.removeprefix("forge:ingots/")}
    # Shared tags are evidence, not sufficient proof of identical gameplay semantics.
    excluded = {"brick", "wax", "wood", "wooden", "long", "all_metal", "neodymium"}
    nonmetals = {"sulfur", "quartz", "nether_quartz", "lapis", "diamond", "emerald",
                 "obsidian", "coal", "charcoal", "salt", "apatite", "sapphire", "ruby",
                 "certus_quartz", "cinnabar", "amethyst", "ender_pearl", "lithium"}

    def add(namespace, filename, kind, members, target):
        ids = items if kind == "Items" else fluids
        if target not in ids:
            raise ValueError("Target absent from ProbeJS: " + target)
        sources = sorted(set(members) - {target})
        if not set(sources).issubset(ids):
            raise ValueError("Source absent from ProbeJS")
        if sources:
            output[f"data/{namespace}/replacements/{filename}.json"].append(
                {"match" + kind: sources, "result" + kind: target})

    def priority(item):
        namespace = item.split(":")[0]
        return ({"minecraft": 0, "gtceu": 1, "thermal": 2,
                 "thermal_extra": 3}.get(namespace, 10), item)

    for material in ("apatite", "cinnabar", "ruby", "sapphire"):
        add("oei", material, "Items", tags["forge:gems/" + material],
            "gtceu:" + material + "_gem")

    for tag, members in sorted(tags.items()):
        parts = tag.split("/")
        if len(parts) != 2:
            continue
        category, material = parts
        if material in excluded:
            continue
        raw_material = material.removeprefix("raw_")
        allowed = material in metals or material in nonmetals
        if category == "forge:storage_blocks":
            allowed = raw_material in metals
        if category not in {"forge:ingots", "forge:nuggets", "forge:dusts", "forge:plates",
                            "forge:rods", "forge:gears", "forge:raw_materials", "forge:storage_blocks"}:
            continue
        if not allowed:
            continue
        # Create's sturdy sheet is a processed component, not an ordinary obsidian plate.
        members = [m for m in members if m in items and m != "create:sturdy_sheet"]
        if not members:
            continue
        if category == "forge:dusts" and material == "quartz":
            members += tags.get("forge:dusts/nether_quartz", [])
        if category == "forge:dusts" and material == "nether_quartz":
            continue
        target = min(members, key=priority)
        if priority(target)[0] == 10:
            continue  # No approved canonical mod: leave the group untouched.
        filename = "metal_blocks" if category == "forge:storage_blocks" else material
        add("oei", filename, "Items", members, target)

    # Ore tags mix many host stones. Only unify matching ordinary/deepslate forms.
    for tag, members in sorted(tags.items()):
        if not tag.startswith("forge:ores/"):
            continue
        material = tag.removeprefix("forge:ores/")
        if "/" in material or material not in metals or material in excluded:
            continue
        for host in ("stone", "deepslate"):
            suffix = material + "_ore"
            paths = {suffix} if host == "stone" else {"deepslate_" + suffix, "deep_" + suffix}
            candidates = [m for m in members if m in items and m.split(":", 1)[1] in paths]
            if not candidates:
                continue
            target = min(candidates, key=priority)
            if priority(target)[0] < 10:
                add("oei", material, "Items", candidates, target)

    # Explicit equivalents only. Preserve GT oil grades and enhanced fuels.
    groups = {
        "crude_oil": ("createdieselgenerators:crude_oil", ["thermal:crude_oil", "gtceu:oil"]),
        "diesel": ("createdieselgenerators:diesel", ["thermal_extra:diesel", "gtceu:diesel"]),
        "gasoline": ("createdieselgenerators:gasoline", ["thermal_extra:gasoline", "gtceu:gasoline"]),
        "plant_oil": ("createdieselgenerators:plant_oil", ["createaddition:seed_oil", "gtceu:seed_oil"]),
        "biodiesel": ("createdieselgenerators:biodiesel", ["gtceu:bio_diesel"]),
        "ethanol": ("createdieselgenerators:ethanol", ["gtceu:ethanol", "createaddition:bioethanol",
                                                   "mekanismgenerators:bioethanol"]),
        "honey": ("create:honey", ["cofh_core:honey", "productivebees:honey",
                                 "the_bumblezone:honey_fluid_still", "brewinandchewin:honey"]),
    }

    def flowing(still):
        namespace, name = still.split(":")
        names = [f"{namespace}:flowing_{name}", f"{namespace}:{name}_flowing"]
        if name.endswith("_still"):
            names.append(f"{namespace}:{name.removesuffix('_still')}_flowing")
        matches = [name for name in names if name in fluids]
        if len(matches) != 1:
            raise ValueError("Ambiguous/missing flowing variant: " + still)
        return matches[0]

    for name, (target, sources) in groups.items():
        add("oef", name, "Fluids", sources, target)
        add("oef", name + "_flowing", "Fluids", [flowing(s) for s in sources], flowing(target))
        buckets = [fluids[s]["bucketItem"] for s in sources if fluids[s]["hasBucket"]
                   and fluids[s]["bucketItem"] != "minecraft:air"]
        add("oei", "buckets", "Items", buckets, fluids[target]["bucketItem"])
    return dict(output)


def patch_text(output):
    existing = {p.relative_to(ROOT).as_posix() for ns in ("oei", "oef")
                for p in (ROOT / "data" / ns / "replacements").glob("*.json")}
    sections = ["*** Begin Patch"]
    for name in sorted(existing | output.keys()):
        if name not in output:
            sections.append("*** Delete File: " + name)
            continue
        content = "[\n" + ",\n".join("  " + json.dumps(rule, ensure_ascii=False)
                                     for rule in output[name]) + "\n]\n"
        path = ROOT / name
        if path.exists():
            old = path.read_text(encoding="utf-8-sig")
            if old == content:
                continue
            sections.extend(["*** Update File: " + name, "@@"])
            sections.extend("-" + line for line in old.splitlines())
        else:
            sections.append("*** Add File: " + name)
        sections.extend("+" + line for line in content.splitlines())
    return "\n".join(sections + ["*** End Patch", ""])


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--patch", type=Path, help="Write a patch for manual review/application")
    args = parser.parse_args()
    output = build_rules()
    if args.patch:
        args.patch.write_text(patch_text(output), encoding="utf-8")
    else:
        patch = patch_text(output)
        if patch != "*** Begin Patch\n*** End Patch\n":
            raise SystemExit("Shipped rules differ from ProbeJS-derived rules; emit --patch to inspect.")
        print("Shipped rules match ProbeJS-derived rules.")
