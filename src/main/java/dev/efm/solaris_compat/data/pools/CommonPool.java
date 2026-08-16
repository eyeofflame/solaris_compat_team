package dev.efm.solaris_compat.data.pools;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.efm.solaris_compat.data.BountyPool;
import dev.efm.solaris_compat.data.KilledEntityData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CommonPool extends BountyPool {
    public static final Codec<CommonPool> CODEC = RecordCodecBuilder.create(
            instance ->
                    instance.group(
                            Codec.STRING.fieldOf("name").forGetter(CommonPool::getName),
                            ItemStack.CODEC.listOf().optionalFieldOf("requireItems", List.of()).forGetter(CommonPool::getRequireItems),
                            KilledEntityData.CODEC.listOf().optionalFieldOf("requireEntities", List.of()).forGetter(CommonPool::getRequireEntities),
                            ItemStack.CODEC.listOf().fieldOf("rewards").forGetter(CommonPool::getRewards)
                    ).apply(instance, CommonPool::new));

    public CommonPool(String name, @Nullable List<ItemStack> requireItems, @Nullable List<KilledEntityData> requireEntities, List<ItemStack> list) {
        super(name, requireItems, requireEntities, list);
    }

    @Override
    public Rarity getRarity() {
        return Rarity.COMMON;
    }
}
