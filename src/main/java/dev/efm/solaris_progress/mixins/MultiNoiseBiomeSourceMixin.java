package dev.efm.solaris_progress.mixins;

import dev.efm.solaris_progress.SolaConfig;
import dev.efm.solaris_progress.worldgen.FlatBlendMath;
import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterLists;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiNoiseBiomeSource.class)
public abstract class MultiNoiseBiomeSourceMixin {

    @Unique
    private static Holder<Biome> solaris$plains;

    @Inject(
            method = "getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;",
            at = @At("HEAD"), cancellable = true)
    private void solaris$forcePlains(int x, int y, int z, Climate.Sampler sampler,
                                     CallbackInfoReturnable<Holder<Biome>> cir) {
        if (!SolaConfig.enabled) return;
        MultiNoiseBiomeSource self = (MultiNoiseBiomeSource) (Object) this;
        if (!self.stable(MultiNoiseBiomeSourceParameterLists.OVERWORLD)) return;

        double bx = x * 4.0;
        double bz = z * 4.0;
        double d = FlatBlendMath.chebyshev(bx - SolaConfig.centerX, bz - SolaConfig.centerZ);
        if (d < SolaConfig.halfExtent + FlatBlendMath.edgeDither(x, z)) {
            Holder<Biome> plains = solaris$plains();
            if (plains != null) {
                cir.setReturnValue(plains);
            }
        }
    }

    @Unique
    private Holder<Biome> solaris$plains() {
        if (solaris$plains == null) {
            for (Holder<Biome> h : ((BiomeSource) (Object) this).possibleBiomes()) {
                if (h.unwrapKey().map(k -> k.equals(Biomes.PLAINS)).orElse(false)) {
                    solaris$plains = h;
                    break;
                }
            }
        }
        return solaris$plains;
    }
}
