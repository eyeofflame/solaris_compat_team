package dev.efm.solaris_progress.mixins;

import dev.efm.solaris_progress.SolaConfig;
import dev.efm.solaris_progress.worldgen.FlatBlendDensityFunction;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {
    @Mutable
    @Shadow
    private RandomState randomState;

    @Shadow
    @Final
    ServerLevel level;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void solaris$wrapOverworldRouter(CallbackInfo ci) {
        if (!SolaConfig.enabled) return;
        if (level.dimension() != Level.OVERWORLD) return;
        RandomStateAccessor acc = (RandomStateAccessor) (Object) this.randomState;
        acc.solaris$setRouter(FlatBlendDensityFunction.wrapRouter(acc.solaris$getRouter()));
    }
}
