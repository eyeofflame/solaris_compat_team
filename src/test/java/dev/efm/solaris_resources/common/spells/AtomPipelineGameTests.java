package dev.efm.solaris_resources.common.spells;

import dev.efm.solaris_resources.common.entity.AtomItemEntity;
import dev.efm.solaris_resources.common.config.AtomConfigs;
import dev.efm.solaris_resources.common.registration.EntityTypeRegistries;
import dev.efm.solaris_resources.common.registration.ItemRegistries;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Files;
import java.nio.file.Path;

@GameTestHolder("solaris_resources")
@PrefixGameTestTemplate(false)
public class AtomPipelineGameTests {
    @GameTest(template = "empty")
    public static void convertOnlyWaterSources(GameTestHelper helper) {
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var level = helper.getLevel();
        BlockPos flowing = origin.east();
        BlockPos waterlogged = origin.west();
        level.setBlock(origin, Blocks.WATER.defaultBlockState(), 2);
        level.setBlock(flowing, Blocks.WATER.defaultBlockState().setValue(BlockStateProperties.LEVEL, 3), 2);
        var stairs = Blocks.OAK_STAIRS.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, true);
        level.setBlock(waterlogged, stairs, 2);

        EffectConvertWater.INSTANCE.onResolveBlock(hit(origin), level, null, null, null, null);

        helper.assertTrue(level.getBlockState(origin).isAir(), "water source must be consumed");
        helper.assertTrue(level.getBlockState(flowing).is(Blocks.WATER), "flowing water must not be consumed");
        helper.assertTrue(level.getBlockState(waterlogged).equals(stairs), "waterlogged blocks must remain");
        int crystals = level.getEntitiesOfClass(ItemEntity.class, SpellEffectRange.WATER.area(origin)).stream()
                .filter(entity -> entity.getItem().is(ItemRegistries.CRYSTALLIZED_WATER.get()))
                .mapToInt(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(crystals == 4, "one water source must yield exactly four crystals, got " + crystals);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void atomCountsSurviveEntitySave(GameTestHelper helper) {
        for (int count : new int[]{65, 128, 255, 256, 512, 1024}) {
            var original = atom(helper, count);
            CompoundTag saved = original.saveWithoutId(new CompoundTag());
            var restored = new AtomItemEntity(EntityTypeRegistries.ATOM_ITEM.get(), helper.getLevel());
            restored.load(saved);
            helper.assertTrue(restored.getItem().getCount() == count,
                    "saved atom count must round-trip without byte truncation: " + count);
            helper.assertTrue(restored.getItem().is(ItemRegistries.UNSTABLE_ATOM.get("hydrogen").get()),
                    "saved atom type must round-trip");
            helper.assertTrue("preserved".equals(restored.getItem().getTag().getString("Probe")),
                    "saved atom tags must round-trip");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void atomCountsSurviveEntityDataPacket(GameTestHelper helper) {
        var original = atom(helper, 1024);
        var received = new AtomItemEntity(EntityTypeRegistries.ATOM_ITEM.get(), helper.getLevel());
        sync(original, received);
        helper.assertTrue(received.getItem().getCount() == 1024,
                "initial entity metadata must synchronize the full atom count");
        original.setItem(original.getItem().copyWithCount(512));
        sync(original, received);
        helper.assertTrue(received.getItem().getCount() == 512,
                "updated entity metadata must synchronize the full atom count");
        original.setItem(ItemStack.EMPTY);
        sync(original, received);
        helper.assertTrue(received.getItem().isEmpty(), "cleared atom metadata must not leave a phantom stack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void dissolveWholeStackPreservesAtomGroups(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var crystals = new ItemEntity(level, origin.getX(), origin.getY() + 2, origin.getZ(),
                new ItemStack(ItemRegistries.CRYSTALLIZED_WATER.get(), 64));
        level.addFreshEntity(crystals);
        EffectDissolveWater.INSTANCE.onResolveBlock(hit(origin), level, null, null, null, null);
        helper.assertTrue(crystals.isRemoved(), "dissolved crystals must be consumed");
        // Forge replaces ordinary atom drops with custom entities on its work queue.
        helper.runAfterDelay(2, () -> {
            var atoms = level.getEntitiesOfClass(AtomItemEntity.class, SpellEffectRange.ABOVE.area(origin));
            helper.assertTrue(atoms.size() == 2, "dissolving must preserve one group per element");
            int hydrogen = 0;
            int oxygen = 0;
            for (var atom : atoms) {
                var restored = new AtomItemEntity(EntityTypeRegistries.ATOM_ITEM.get(), level);
                restored.load(atom.saveWithoutId(new CompoundTag()));
                if (restored.getItem().is(ItemRegistries.UNSTABLE_ATOM.get("hydrogen").get())) {
                    hydrogen += restored.getItem().getCount();
                } else if (restored.getItem().is(ItemRegistries.UNSTABLE_ATOM.get("oxygen").get())) {
                    oxygen += restored.getItem().getCount();
                }
            }
            helper.assertTrue(hydrogen == 1024 && oxygen == 512,
                    "64 crystals must yield save-safe groups of 1024 hydrogen and 512 oxygen");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void loadLegacyAtomStack(GameTestHelper helper) {
        CompoundTag saved = new CompoundTag();
        saved.putInt("Age", 100);
        saved.putInt("Lifespan", 1200);
        saved.put("Item", new ItemStack(ItemRegistries.UNSTABLE_ATOM.get("hydrogen").get(), 32)
                .save(new CompoundTag()));
        var restored = new AtomItemEntity(EntityTypeRegistries.ATOM_ITEM.get(), helper.getLevel());
        restored.load(saved);
        helper.assertTrue(restored.getItem().getCount() == 32, "valid old atom stacks must still load");
        helper.assertTrue(restored.getAge() == 100, "loading must preserve atom age");
        helper.succeed();
    }

    @GameTest(template = "empty", batch = "atom_aggregation")
    public static void aggregateSplitsOrdinaryDrops(GameTestHelper helper) throws Exception {
        Path config = FMLPaths.CONFIGDIR.get().resolve("solaris_resources/iron.json");
        String originalConfig = Files.readString(config);
        try {
            Files.writeString(config, "{\"convertedItem\":\"minecraft:raw_iron\",\"ratio\":8}");
            var loaded = AtomConfigs.STORE.reload();
            helper.assertTrue(loaded.failed() == 0, "aggregation fixture config must load");
            aggregateRawIron(helper);
        } finally {
            Files.writeString(config, originalConfig);
            AtomConfigs.STORE.reload();
        }
        helper.succeed();
    }

    private static void aggregateRawIron(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos origin = helper.absolutePos(new BlockPos(2, 1, 2));
        var iron = new AtomItemEntity(level, origin.getX(), origin.getY() + 2, origin.getZ(),
                new ItemStack(ItemRegistries.UNSTABLE_ATOM.get("iron").get(), 1040));
        level.addFreshEntity(iron);

        EffectAggregationAtom.INSTANCE.onResolveBlock(hit(origin), level, null, null, null, null);

        var drops = level.getEntitiesOfClass(ItemEntity.class, SpellEffectRange.ABOVE.area(origin)).stream()
                .filter(entity -> entity.getItem().is(Items.RAW_IRON)).toList();
        helper.assertTrue(drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum() == 130,
                "1040 iron atoms must produce 130 raw iron without loss");
        helper.assertTrue(drops.stream().allMatch(entity -> entity.getItem().getCount() <= entity.getItem().getMaxStackSize()),
                "ordinary drops must fit their maximum stack size");
        helper.assertTrue(iron.isRemoved(), "fully aggregated atoms must be removed");
    }

    private static AtomItemEntity atom(GameTestHelper helper, int count) {
        var stack = new ItemStack(ItemRegistries.UNSTABLE_ATOM.get("hydrogen").get(), count);
        stack.getOrCreateTag().putString("Probe", "preserved");
        return new AtomItemEntity(helper.getLevel(), 0, 0, 0, stack);
    }

    private static void sync(AtomItemEntity original, AtomItemEntity received) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            new ClientboundSetEntityDataPacket(original.getId(), original.getEntityData().packDirty()).write(buffer);
            var decoded = new ClientboundSetEntityDataPacket(buffer);
            received.getEntityData().assignValues(decoded.packedItems());
        } finally {
            buffer.release();
        }
    }

    private static BlockHitResult hit(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
}
