package dev.efm.solaris_progress.mixins;

import dev.efm.solaris_progress.SolaConfig;
import dev.efm.solaris_progress.worldgen.InitPlacementMath;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.core.RegistryAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkGenerator.class)
public abstract class ChunkGeneratorMixin {
    @Inject(method = "createStructures", at = @At("HEAD"), cancellable = true)
    private void solaris$blockSpawnRegionStructures(
            RegistryAccess registryAccess,
            ChunkGeneratorStructureState structureState,
            StructureManager structureManager,
            ChunkAccess chunk,
            StructureTemplateManager structureTemplateManager,
            CallbackInfo ci) {
        if (!SolaConfig.enabled) return;
        if (!(StructureManagerAccessor.class.cast(structureManager).solaris$getLevel() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;

        var chunkPos = chunk.getPos();
        if (InitPlacementMath.chunkIntersectsProtectedRegion(
                chunkPos.x, chunkPos.z,
                (int) SolaConfig.centerX, (int) SolaConfig.centerZ,
                (int) SolaConfig.halfExtent, SolaConfig.structureBlockMargin)) {
            ci.cancel();
        }
    }
}
