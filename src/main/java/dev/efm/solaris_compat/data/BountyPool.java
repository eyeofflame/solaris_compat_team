package dev.efm.solaris_compat.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BountyPool {
    private final String id;

    @Nullable
    private final List<ItemStack> requireItems;

    @Nullable
    private final List<KilledEntityData> requireEntities;

    private final List<ItemStack> rewards;

    public static final Codec<BountyPool> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(BountyPool::getName),
                    ItemStack.CODEC.listOf().optionalFieldOf("requireItems", List.of()).forGetter(BountyPool::getRequireItems),
                    KilledEntityData.CODEC.listOf().optionalFieldOf("requireEntities", List.of()).forGetter(BountyPool::getRequireEntities),
                    ItemStack.CODEC.listOf().fieldOf("rewards").forGetter(BountyPool::getRewards)
            ).apply(instance, BountyPool::new)
    );

    public BountyPool(String name, @Nullable List<ItemStack> requireItems, @Nullable List<KilledEntityData> requireEntities, List<ItemStack> list) {
        this.id = name;
        this.rewards = list;
        this.requireItems = requireItems;
        this.requireEntities = requireEntities;
    }

    public String getName() {
        return this.id;
    }

    public List<ItemStack> getRewards() {
        return this.rewards;
    }

    public @Nullable List<ItemStack> getRequireItems() {
        return requireItems;
    }

    public @Nullable List<KilledEntityData> getRequireEntities() {
        return requireEntities;
    }

    public Rarity getRarity() {
        return null;
    }
}
