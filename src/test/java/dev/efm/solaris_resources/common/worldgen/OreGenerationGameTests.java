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

@GameTestHolder("solaris_resources")
@PrefixGameTestTemplate(false)
public class OreGenerationGameTests {
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
