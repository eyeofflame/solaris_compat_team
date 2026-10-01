package dev.efm.solaris_core.mixin;

import dev.efm.solaris_core.functions.structureReplacement.DataLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

@Mixin(StructureTemplate.class)
public abstract class StructureTemplateMixin {
    @Inject(method = "placeInWorld", at = @At("HEAD"))
    private void replaceBlocks(ServerLevelAccessor pServerLevel, BlockPos pOffset, BlockPos pPos, StructurePlaceSettings pSettings, RandomSource pRandom, int pFlags, CallbackInfoReturnable<Boolean> cir) {
        StructureTemplate self = (StructureTemplate) (Object) this;
        DataLoader.ReplacementDataLoader loader = DataLoader.ReplacementDataLoader.INSTANCE;

        if (loader.isEmpty()) return;

        for (StructureTemplate.Palette palette : self.palettes) {
            List<StructureTemplate.StructureBlockInfo> blocks = palette.blocks;

            ListIterator<StructureTemplate.StructureBlockInfo> it = blocks.listIterator();
            while (it.hasNext()) {
                StructureTemplate.StructureBlockInfo info = it.next();
                BlockState state = info.state();

                ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                ResourceLocation replacementId = loader.getReplacement(blockId);
                if (replacementId != null) {
                    BlockState newState = solaris_core$copyProperties(state, Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(replacementId)).defaultBlockState());
                    it.set(new StructureTemplate.StructureBlockInfo(info.pos(), newState, info.nbt()));
                }
            }
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private BlockState solaris_core$copyProperties(BlockState from, BlockState to) {
        for (var prop : from.getProperties()) {
            if (to.hasProperty(prop)) {
                to = to.setValue((Property) prop, (Comparable) from.getValue(prop));
            }
        }
        return to;
    }
}
