package dev.efm.solaris_compat.common.recipeType.ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Objects;

public class IngredientEntry {
    public final Ingredient ingredient;
    public final int damage;
    public final ItemStack remainder;

    public IngredientEntry(Ingredient ingredient, int damage, ItemStack remainder) {
        this.ingredient = ingredient;
        this.damage = damage;
        this.remainder = remainder;
    }

    public static IngredientEntry fromJson(JsonElement element) {
        JsonObject obj = element.getAsJsonObject();
        int damage = obj.has("damage") ? obj.get("damage").getAsInt() : 0;
        ItemStack remainder = ItemStack.EMPTY;
        if (obj.has("remainder")) {
            JsonElement rem = obj.get("remainder");
            if (rem.isJsonPrimitive()) {
                remainder = new ItemStack(
                        Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.bySeparator(rem.getAsString(), ':')))
                );
            } else {
                JsonObject remObj = rem.getAsJsonObject();
                remainder = new ItemStack(
                        Objects.requireNonNull(ForgeRegistries.ITEMS.getValue(ResourceLocation.bySeparator(remObj.get("item").getAsString(), ':'))),
                        remObj.has("count") ? remObj.get("count").getAsInt() : 1
                );
            }

            obj.remove("remainder");
        }
        obj.remove("damage");

        Ingredient ingredient = Ingredient.fromJson(obj);
        return new IngredientEntry(ingredient,damage,remainder);
    }

    public Ingredient toIngredient(){
        return this.ingredient;
    }
}
