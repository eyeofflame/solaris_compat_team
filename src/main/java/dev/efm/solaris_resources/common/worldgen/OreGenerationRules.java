package dev.efm.solaris_resources.common.worldgen;

import com.mojang.logging.LogUtils;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.config.WorldgenConfigStore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

public final class OreGenerationRules {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final WorldgenConfigStore CONFIG = new WorldgenConfigStore(
            FMLPaths.CONFIGDIR.get().resolve(SolarisResources.MODID).resolve("worldgen.json"),
            value -> {
                ResourceLocation id = ResourceLocation.tryParse(value);
                return value.contains(":") && id != null && ForgeRegistries.BLOCKS.containsKey(id);
            },
            (file, error) -> LOGGER.error("Failed to load worldgen config {}; keeping previous settings", file, error));

    private OreGenerationRules() { }

    public static boolean reload() {
        return CONFIG.reload();
    }

    public static boolean isOre(BlockState state) {
        return OreGenerationPolicy.isOre(ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString(),
                state.is(Tags.Blocks.ORES), CONFIG.additionalOreBlocks());
    }

    @Nullable
    public static BlockState filterVeinResult(@Nullable BlockState state) {
        if (state == null) return null;
        // A null filler result falls back to the noise generator's normal terrain, not air.
        return isOre(state) || state.is(Blocks.RAW_IRON_BLOCK) || state.is(Blocks.RAW_COPPER_BLOCK) ? null : state;
    }
}
