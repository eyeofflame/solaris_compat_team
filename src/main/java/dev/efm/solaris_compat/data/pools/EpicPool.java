package dev.efm.solaris_compat.data.pools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.efm.solaris_compat.data.BountyPool;
import dev.efm.solaris_compat.data.KilledEntityData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EpicPool extends BountyPool {
    public static final Codec<EpicPool> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(EpicPool::getName),
                    ItemStack.CODEC.listOf().optionalFieldOf("requireItems",List.of()).forGetter(EpicPool::getRequireItems),
                    KilledEntityData.CODEC.listOf().optionalFieldOf("requireEntities",List.of()).forGetter(EpicPool::getRequireEntities),
                    ItemStack.CODEC.listOf().fieldOf("rewards").forGetter(EpicPool::getRewards)
            ).apply(instance, EpicPool::new)
    );

    public EpicPool(String name, @Nullable List<ItemStack> requireItems, @Nullable List<KilledEntityData> requireEntities, List<ItemStack> list) {
        super(name, requireItems, requireEntities, list);
    }

    @Override
    public Rarity getRarity() {
        return Rarity.EPIC;
    }
}
