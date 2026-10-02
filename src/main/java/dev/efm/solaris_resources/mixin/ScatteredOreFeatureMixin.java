package dev.efm.solaris_resources.mixin;

import dev.efm.solaris_resources.common.worldgen.OreGenerationRules;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.ScatteredOreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.function.Function;

@Mixin(ScatteredOreFeature.class)
public abstract class ScatteredOreFeatureMixin {
    @Redirect(method = "place", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/levelgen/feature/OreFeature;canPlaceOre(Lnet/minecraft/world/level/block/state/BlockState;Ljava/util/function/Function;Lnet/minecraft/util/RandomSource;Lnet/minecraft/world/level/levelgen/feature/configurations/OreConfiguration;Lnet/minecraft/world/level/levelgen/feature/configurations/OreConfiguration$TargetBlockState;Lnet/minecraft/core/BlockPos$MutableBlockPos;)Z"), require = 1)
    private boolean solaris$checkTarget(BlockState existing, Function<BlockPos, BlockState> adjacent,
            RandomSource random, OreConfiguration config, OreConfiguration.TargetBlockState target,
            BlockPos.MutableBlockPos pos, FeaturePlaceContext<OreConfiguration> context) {
        if (context.level() instanceof WorldGenRegion && OreGenerationRules.isOre(target.state)) return false;
        return OreFeature.canPlaceOre(existing, adjacent, random, config, target, pos);
    }
}
