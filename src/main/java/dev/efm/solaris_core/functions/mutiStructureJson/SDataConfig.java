package dev.efm.solaris_core.functions.mutiStructureJson;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class SDataConfig {
    public record WithCore(ResourceLocation id, BlockPos pos, ResourceLocation coreBlockId) {
        public static final Codec<WithCore> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ResourceLocation.CODEC.fieldOf("structure_id").forGetter(WithCore::id),
                        BlockPos.CODEC.fieldOf("core_pos").forGetter(WithCore::pos),
                        ResourceLocation.CODEC.fieldOf("core_block_id").forGetter(WithCore::coreBlockId)
                ).apply(instance, WithCore::new)
        );
    }

    public record NoCore(ResourceLocation id, BlockPos pos) {
        public static final Codec<NoCore> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ResourceLocation.CODEC.fieldOf("structure_id").forGetter(NoCore::id),
                        BlockPos.CODEC.fieldOf("check_pos").forGetter(NoCore::pos)
                ).apply(instance, NoCore::new)
        );
    }
}
