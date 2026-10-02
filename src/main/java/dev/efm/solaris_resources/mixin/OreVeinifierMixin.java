package dev.efm.solaris_resources.mixin;

import dev.efm.solaris_resources.common.worldgen.OreGenerationRules;
import net.minecraft.world.level.levelgen.DensityFunction;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.OreVeinifier;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(OreVeinifier.class)
public abstract class OreVeinifierMixin {
    @Inject(method = "create", at = @At("RETURN"), cancellable = true, require = 1)
    private static void solaris$filterVein(DensityFunction toggle, DensityFunction ridged, DensityFunction gap,
            PositionalRandomFactory random, CallbackInfoReturnable<NoiseChunk.BlockStateFiller> callback) {
        NoiseChunk.BlockStateFiller original = callback.getReturnValue();
        callback.setReturnValue(context -> OreGenerationRules.filterVeinResult(original.calculate(context)));
    }
}
