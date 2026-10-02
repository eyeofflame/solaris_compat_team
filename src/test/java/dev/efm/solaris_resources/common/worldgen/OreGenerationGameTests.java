package dev.efm.solaris_resources.common.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.level.storage.LevelResource;
import dev.efm.solaris_resources.common.registration.ItemRegistries;

@GameTestHolder("solaris_resources")
@PrefixGameTestTemplate(false)
public class OreGenerationGameTests {
    @GameTest(template = "empty", batch = "tag_reload", timeoutTicks = 1200)
    public static void datapackTagReload(GameTestHelper helper) throws Exception {
        var server = helper.getLevel().getServer();
        var repository = server.getPackRepository();
        List<String> originalPacks = List.copyOf(repository.getSelectedIds());
        Path pack = server.getWorldPath(LevelResource.DATAPACK_DIR).resolve("solaris_tag_reload_probe");
        Path tagFile = pack.resolve("data/forge/tags/blocks/ores.json");
        Block sample = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("ars_nouveau:source_gem_block"));
        try {
            Files.createDirectories(tagFile.getParent());
            Files.writeString(pack.resolve("pack.mcmeta"), "{\"pack\":{\"pack_format\":15,\"description\":\"Temporary ore tag reload test\"}}");
            Files.writeString(tagFile, "{\"replace\":true,\"values\":[]}");
            repository.reload();
            var selected = new ArrayList<>(originalPacks);
            selected.add("file/solaris_tag_reload_probe");
            server.reloadResources(selected).join();
            helper.assertFalse(OreGenerationRules.isOre(sample.defaultBlockState()), "tag removal after resource reload must take effect");
            Files.writeString(tagFile, "{\"replace\":true,\"values\":[\"ars_nouveau:source_gem_block\"]}");
            server.reloadResources(selected).join();
            helper.assertTrue(OreGenerationRules.isOre(sample.defaultBlockState()), "tag addition after resource reload must take effect");
        } finally {
            server.reloadResources(originalPacks).join();
            try (var paths = Files.walk(pack)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.delete(path);
            }
            repository.reload();
        }
        helper.assertTrue(OreGenerationRules.isOre(sample.defaultBlockState()), "original test tag restored");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void taggedModOre(GameTestHelper helper) {
        Block sample = ForgeRegistries.BLOCKS.getValue(ResourceLocation.parse("ars_nouveau:source_gem_block"));
        helper.assertTrue(sample != null && sample != Blocks.AIR, "sample mod block exists");
        helper.assertTrue(OreGenerationRules.isOre(sample.defaultBlockState()), "test datapack ore tag recognized");
        WorldGenRegion region = region(helper);
        BlockPos pos = new BlockPos(8, 16, 8);
        helper.assertFalse(region.setBlock(pos, sample.defaultBlockState(), 2), "tagged mod block writes rejected");
        var feature = helper.getLevel().registryAccess().registryOrThrow(Registries.CONFIGURED_FEATURE)
                .get(ResourceLocation.parse("solaris_resources_test:tagged_ore"));
        helper.assertTrue(feature != null, "test configured feature loaded");
        feature.place(region, helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(123), pos);
        helper.assertTrue(count(region, sample) == 0, "tagged mod feature suppressed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void commandReloadAndFailureIsolation(GameTestHelper helper) throws Exception {
        Path dir = FMLPaths.CONFIGDIR.get().resolve("solaris_resources");
        Path worldgen = dir.resolve("worldgen.json");
        Path iron = dir.resolve("iron.json");
        String savedWorldgen = Files.readString(worldgen);
        String savedIron = Files.readString(iron);
        var commands = helper.getLevel().getServer().getCommands();
        var source = helper.getLevel().getServer().createCommandSourceStack();
        var sharedIron = ItemRegistries.STABLE_ATOM.get("iron").get();
        try {
            Files.writeString(worldgen, "{\"additionalOreBlocks\":[\"minecraft:gold_block\"]}");
            try {
                commands.getDispatcher().execute("solaris_resources_reload", source.withPermission(0));
                helper.fail("unprivileged reload must be rejected");
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException expected) { }
            helper.assertFalse(OreGenerationRules.isOre(Blocks.GOLD_BLOCK.defaultBlockState()), "denied command leaves config unchanged");
            Files.writeString(iron, "{");
            int result = commands.getDispatcher().execute("solaris_resources_reload", source.withPermission(2));
            helper.assertTrue(result == 0, "partial failure reported");
            helper.assertTrue(OreGenerationRules.isOre(Blocks.GOLD_BLOCK.defaultBlockState()), "atom failure must not block worldgen reload");
            WorldGenRegion region = region(helper);
            helper.assertFalse(region.setBlock(new BlockPos(8, 16, 8), Blocks.GOLD_BLOCK.defaultBlockState(), 2), "additional block immediately blocked");
            Files.writeString(iron, "{\"convertedItem\":\"minecraft:iron_ingot\",\"ratio\":4}");
            Files.writeString(worldgen, "{");
            result = commands.getDispatcher().execute("solaris_resources_reload", source);
            helper.assertTrue(result == 0, "worldgen failure reported");
            helper.assertTrue(sharedIron.getConversionSettings().ratio() == 4, "worldgen failure must not block atom reload");
            helper.assertTrue(ItemRegistries.UNSTABLE_ATOM.get("iron").get().getConversionSettings().ratio() == 4, "both atom variants update");
            helper.assertTrue(OreGenerationRules.isOre(Blocks.GOLD_BLOCK.defaultBlockState()), "bad config retains last valid worldgen settings");
            Files.writeString(worldgen, "{\"additionalOreBlocks\":[]}");
            helper.assertTrue(commands.getDispatcher().execute("solaris_resources_reload", source) == 1, "console reload succeeds");
            helper.assertFalse(OreGenerationRules.isOre(Blocks.GOLD_BLOCK.defaultBlockState()), "removed entry immediately allowed");
            helper.assertTrue(region.setBlock(new BlockPos(8, 16, 8), Blocks.GOLD_BLOCK.defaultBlockState(), 2), "removed entry writes succeed");
            Files.writeString(worldgen, "{\"additionalOreBlocks\":[\":gold_block\"]}");
            helper.assertTrue(commands.getDispatcher().execute("solaris_resources_reload", source) == 0,
                    "empty namespace must be rejected, not report a successful ineffective reload");
        } finally {
            Files.writeString(worldgen, savedWorldgen);
            Files.writeString(iron, savedIron);
            commands.getDispatcher().execute("solaris_resources_reload", source);
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void generatedNetherChunks(GameTestHelper helper) {
        var nether = helper.getLevel().getServer().getLevel(Level.NETHER);
        helper.assertTrue(nether != null, "nether exists");
        int ordinary = 0;
        for (int z = 200; z < 202; z++) {
            for (int x = 200; x < 202; x++) {
                var chunk = nether.getChunk(x, z);
                for (var section : chunk.getSections()) {
                    for (int ly = 0; ly < 16; ly++) for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
                        var state = section.getBlockState(lx, ly, lz);
                        helper.assertFalse(OreGenerationRules.isOre(state), "naturally generated nether chunk contains ore");
                        if (state.is(Blocks.NETHERRACK)) ordinary++;
                    }
                }
            }
        }
        helper.assertTrue(ordinary > 0, "nether terrain is preserved");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 1200)
    public static void generatedNoiseOverworldChunks(GameTestHelper helper) {
        var dimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse("solaris_resources_test:noise_overworld"));
        var level = helper.getLevel().getServer().getLevel(dimension);
        helper.assertTrue(level != null, "test noise overworld dimension exists");
        int substrate = 0;
        int ordinaryRock = 0;
        for (int z = 0; z < 2; z++) for (int x = 0; x < 2; x++) {
            var chunk = level.getChunk(x, z);
            for (var section : chunk.getSections()) {
                for (int ly = 0; ly < 16; ly++) for (int lx = 0; lx < 16; lx++) for (int lz = 0; lz < 16; lz++) {
                    var state = section.getBlockState(lx, ly, lz);
                    helper.assertFalse(OreGenerationRules.isOre(state), "generated noise overworld contains ore");
                    helper.assertFalse(state.is(Blocks.RAW_IRON_BLOCK) || state.is(Blocks.RAW_COPPER_BLOCK), "generated vein contains raw resource");
                    if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE)) substrate++;
                    if (state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE)
                            || state.is(Blocks.TUFF) || state.is(Blocks.DIRT) || state.is(Blocks.GRAVEL)) ordinaryRock++;
                }
            }
        }
        helper.assertTrue(substrate > 0, "stone substrate preserved");
        helper.assertTrue(ordinaryRock > 0, "ordinary geology preserved");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void worldgenWrites(GameTestHelper helper) {
        WorldGenRegion region = region(helper);
        BlockPos pos = new BlockPos(8, 16, 8);
        helper.assertFalse(region.setBlock(pos, Blocks.IRON_ORE.defaultBlockState(), 2), "ore write must fail");
        helper.assertTrue(region.getBlockState(pos).is(Blocks.STONE), "ore write must preserve stone");
        helper.assertTrue(region.setBlock(pos, Blocks.GRANITE.defaultBlockState(), 2), "ordinary rock write succeeds");
        helper.assertTrue(region.getBlockState(pos).is(Blocks.GRANITE), "rock is written");
        helper.assertTrue(region.setBlock(pos, Blocks.RAW_IRON_BLOCK.defaultBlockState(), 2), "raw block not globally banned");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void standardAndScattered(GameTestHelper helper) {
        for (var feature : List.of(Feature.ORE, Feature.SCATTERED_ORE)) {
            WorldGenRegion region = region(helper);
            var targets = List.of(OreConfiguration.target(new BlockMatchTest(Blocks.STONE), Blocks.IRON_ORE.defaultBlockState()),
                    OreConfiguration.target(new BlockMatchTest(Blocks.STONE), Blocks.GRANITE.defaultBlockState()));
            feature.place(new OreConfiguration(targets, 32, 0), region,
                    helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(123), new BlockPos(8, 16, 8));
            helper.assertTrue(count(region, Blocks.IRON_ORE) == 0, "ore must not generate: " + feature);
            helper.assertTrue(count(region, Blocks.GRANITE) > 0, "mixed target ordinary rock must generate: " + feature);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void normalLevelWrites(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(1, 2, 1));
        helper.getLevel().setBlock(pos, Blocks.IRON_ORE.defaultBlockState(), 2);
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.IRON_ORE), "normal level ore placement allowed");
        // Runtime/admin Feature use must also remain unrestricted.
        BlockPos center = pos.above(10);
        for (BlockPos fill : BlockPos.betweenClosed(center.offset(-8, -6, -8), center.offset(8, 6, 8))) {
            helper.getLevel().setBlock(fill, Blocks.STONE.defaultBlockState(), 2);
        }
        boolean placed = Feature.ORE.place(new OreConfiguration(new BlockMatchTest(Blocks.STONE), Blocks.IRON_ORE.defaultBlockState(), 16),
                helper.getLevel(), helper.getLevel().getChunkSource().getGenerator(), RandomSource.create(123), center);
        helper.assertTrue(placed, "runtime OreFeature must remain allowed");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void noiseVeins(GameTestHelper helper) throws Exception {
        helper.assertTrue(OreGenerationRules.filterVeinResult(null) == null, "null unchanged");
        for (Block ore : List.of(Blocks.IRON_ORE, Blocks.RAW_IRON_BLOCK, Blocks.RAW_COPPER_BLOCK)) {
            helper.assertTrue(OreGenerationRules.filterVeinResult(ore.defaultBlockState()) == null, "noise resource filtered");
        }
        helper.assertTrue(OreGenerationRules.filterVeinResult(Blocks.TUFF.defaultBlockState()).is(Blocks.TUFF), "tuff preserved");
        var create = OreVeinifier.class.getDeclaredMethod("create", DensityFunction.class, DensityFunction.class,
                DensityFunction.class, PositionalRandomFactory.class);
        create.setAccessible(true);
        int rockCount = 0;
        for (double toggle : new double[]{0.8, -0.8}) {
            var filler = (NoiseChunk.BlockStateFiller) create.invoke(null, DensityFunctions.constant(toggle),
                    DensityFunctions.constant(-1), DensityFunctions.constant(1), new XoroshiroRandomSource(123).forkPositional());
            for (int x = 0; x < 4096; x++) {
                var state = filler.calculate(new DensityFunction.SinglePointContext(x, toggle > 0 ? 24 : -32, 0));
                if (state != null) {
                    helper.assertFalse(OreGenerationRules.isOre(state) || state.is(Blocks.RAW_IRON_BLOCK)
                            || state.is(Blocks.RAW_COPPER_BLOCK), "actual OreVeinifier filler must not yield resources");
                    rockCount++;
                }
            }
        }
        helper.assertTrue(rockCount > 0, "noise vein filler rock must remain");
        helper.succeed();
    }

    static WorldGenRegion region(GameTestHelper helper) {
        List<ChunkAccess> chunks = new ArrayList<>();
        for (int z = -1; z <= 1; z++) {
            for (int x = -1; x <= 1; x++) {
                var chunk = new ProtoChunk(new ChunkPos(x, z), UpgradeData.EMPTY, helper.getLevel(),
                        helper.getLevel().registryAccess().registryOrThrow(Registries.BIOME), null);
                for (BlockPos pos : BlockPos.betweenClosed(x * 16, 0, z * 16, x * 16 + 15, 32, z * 16 + 15)) {
                    chunk.setBlockState(pos, Blocks.STONE.defaultBlockState(), false);
                }
                chunk.setStatus(ChunkStatus.CARVERS);
                chunks.add(chunk);
            }
        }
        return new WorldGenRegion(helper.getLevel(), chunks, ChunkStatus.FEATURES, 1);
    }

    static int count(WorldGenRegion region, Block block) {
        int count = 0;
        for (BlockPos pos : BlockPos.betweenClosed(-16, 0, -16, 31, 32, 31)) {
            if (region.getBlockState(pos).is(block)) count++;
        }
        return count;
    }
}
