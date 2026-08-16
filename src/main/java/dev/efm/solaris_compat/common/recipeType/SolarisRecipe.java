package dev.efm.solaris_compat.common.recipeType;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.efm.solaris_compat.common.SRegistry;
import dev.efm.solaris_compat.common.recipeType.ingredient.IngredientEntry;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SolarisRecipe extends ShapelessRecipe {
    private final List<IngredientEntry> entries;

    public SolarisRecipe(ResourceLocation pId, String pGroup, CraftingBookCategory pCategory, ItemStack pResult, List<IngredientEntry> entries) {
        super(
                pId, pGroup, pCategory, pResult,
                entries.stream().map(IngredientEntry::toIngredient).collect(NonNullList::create, NonNullList::add, NonNullList::addAll)
        );

        this.entries = entries;
    }

    @Override
    public @NotNull NonNullList<ItemStack> getRemainingItems(@NotNull CraftingContainer pContainer) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(pContainer.getContainerSize(), ItemStack.EMPTY);

        for (int i = 0; i < pContainer.getContainerSize(); i++) {
            ItemStack stack = pContainer.getItem(i);

            if (stack.isEmpty()) {
                continue;
            }

            IngredientEntry entry = findMatchingEntry(stack);

            if (entry == null) continue;

            if (entry.damage > 0) {
                ItemStack copy = stack.copy();
                int newDamage = copy.getDamageValue() + entry.damage;
                if (newDamage < copy.getMaxDamage()) {
                    copy.setDamageValue(newDamage);
                    remaining.set(i, copy);
                }
            } else if (!entry.remainder.isEmpty()) {
                remaining.set(i, entry.remainder.copy());
            } else if (stack.hasCraftingRemainingItem()) {
                remaining.set(i, stack.getCraftingRemainingItem());
            }
        }
        return remaining;
    }

    private IngredientEntry findMatchingEntry(ItemStack stack) {
        for (IngredientEntry entry : entries) {
            if (entry.ingredient.test(stack)) {
                return entry;
            }
        }
        return null;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return SRegistry.SOLARIS_RECIPE_SERIALIZER.get();
    }

    public static class Serializer implements RecipeSerializer<SolarisRecipe> {
        @Override
        public @NotNull SolarisRecipe fromJson(@NotNull ResourceLocation pRecipeId, @NotNull JsonObject json) {
            String group = json.has("group") ? json.get("group").getAsString() : "";
            CraftingBookCategory category = CraftingBookCategory.CODEC.byName(
                    json.has("category") ? json.get("category").getAsString() : "misc"
            );

            List<IngredientEntry> entries = new ArrayList<>();
            JsonArray ingredients = json.getAsJsonArray("ingredients");

            for (JsonElement element : ingredients) {
                entries.add(IngredientEntry.fromJson(element));
            }

            ItemStack result = ShapedRecipe.itemStackFromJson(json.getAsJsonObject("result"));
            return new SolarisRecipe(pRecipeId, group, category, result, entries);
        }

        @Override
        public @Nullable SolarisRecipe fromNetwork(@NotNull ResourceLocation pRecipeId, @NotNull FriendlyByteBuf buf) {
            String group = buf.readUtf();
            CraftingBookCategory category = buf.readEnum(CraftingBookCategory.class);
            int count = buf.readVarInt();
            List<IngredientEntry> entries = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Ingredient ingredient = Ingredient.fromNetwork(buf);
                int damage = buf.readVarInt();
                ItemStack remainder = buf.readItem();
                entries.add(new IngredientEntry(ingredient, damage, remainder));
            }
            ItemStack result = buf.readItem();
            return new SolarisRecipe(pRecipeId, group, category, result, entries);
        }

        @Override
        public void toNetwork(@NotNull FriendlyByteBuf pBuffer, @NotNull SolarisRecipe pRecipe) {
            pBuffer.writeUtf(pRecipe.getGroup());
            pBuffer.writeEnum(pRecipe.category());
            pBuffer.writeVarInt(pRecipe.entries.size());

            for (IngredientEntry entry : pRecipe.entries){
                entry.ingredient.toNetwork(pBuffer);
                pBuffer.writeVarInt(entry.damage);
                pBuffer.writeItem(entry.remainder);
            }

            pBuffer.writeItem(pRecipe.getResultItem(RegistryAccess.EMPTY));
        }
    }
}
