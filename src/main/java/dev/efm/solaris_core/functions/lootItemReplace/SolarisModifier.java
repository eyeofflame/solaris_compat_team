package dev.efm.solaris_core.functions.lootItemReplace;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class SolarisModifier extends LootModifier {
    private final Map<Item, Item> replacements;

    public static final Codec<SolarisModifier> CODEC = RecordCodecBuilder.create(instance ->
            LootModifier.codecStart(instance).and(
                            Codec.unboundedMap(ForgeRegistries.ITEMS.getCodec(), ForgeRegistries.ITEMS.getCodec()).optionalFieldOf("replacements", new HashMap<>()).forGetter(m -> m.replacements)
                    )
                    .apply(instance, SolarisModifier::new)
    );

    /**
     * Constructs a LootModifier.
     *
     * @param conditionsIn the ILootConditions that need to be matched before the loot is modified.
     */
    protected SolarisModifier(LootItemCondition[] conditionsIn, Map<Item, Item> replacements) {
        super(conditionsIn);
        this.replacements = replacements;
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        for (int i = 0; i < generatedLoot.size(); i++) {
            ItemStack stack = generatedLoot.get(i);
            Item replacement = replacements.get(stack.getItem());

            if (replacement != null) {
                generatedLoot.set(i, new ItemStack(replacement, stack.getCount()));
            }
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
