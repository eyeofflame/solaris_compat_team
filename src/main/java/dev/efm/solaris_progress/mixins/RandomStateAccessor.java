package dev.efm.solaris_progress.mixins;

import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RandomState.class)
public interface RandomStateAccessor {
    @Mutable
    @Accessor("router")
    void solaris$setRouter(NoiseRouter router);

    @Accessor("router")
    NoiseRouter solaris$getRouter();
}
