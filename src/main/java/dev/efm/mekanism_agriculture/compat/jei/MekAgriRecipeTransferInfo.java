package dev.efm.mekanism_agriculture.compat.jei;

import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import com.blakebr0.mysticalagriculture.compat.jei.category.InfusionCategory;
import dev.efm.mekanism_agriculture.common.registration.MekContainerTypes;
import dev.efm.mekanism_agriculture.common.registration.TileEntityMekInfusioner;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mekanism.api.inventory.IInventorySlot;
import mekanism.common.inventory.container.slot.InventoryContainerSlot;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * JEI 的「+」一键转移:把玩家背包里的材料按配方填进注魔机的 9 个输入槽。
 *
 * <p>这台机器有 9 个输入槽,手摆很烦,所以走 JEI 的 {@link IRecipeTransferInfo} 让 JEI 自己算点击序列。
 *
 * <h2>槽位对齐是这里唯一容易错的地方</h2>
 * JEI 是<b>按索引一对一</b>映射配方槽位与容器槽位的,所以 {@link #getRecipeSlots} 的顺序必须和神秘农业
 * JEI 类别里 {@code addSlot} 的顺序严格一致 —— 那边是先加中心槽 {@code getIngredients().get(0)},
 * 再加索引 1..8 的八座基座。我们的方块实体也是槽 0 = 中央核心物、1..8 = 辅料,正好对得上。
 * 顺序错了的话「+」会把核心物填进辅料槽,机器就永远匹配不上。
 *
 * <p>配方里有重复物品(比如 4 个金苹果 + 4 份精华)也不用特殊处理:JEI 会把同一个玩家堆叠
 * 拆成多次点击分填到多个槽位,这是它的既有行为(原版合成配方同样依赖这一点)。
 */
public class MekAgriRecipeTransferInfo implements IRecipeTransferInfo<MekanismTileContainer<TileEntityMekInfusioner>, IInfusionRecipe> {

    @SuppressWarnings("unchecked")
    @Override
    public Class<MekanismTileContainer<TileEntityMekInfusioner>> getContainerClass() {
        // MekanismTileContainer 是泛型,这里只能做非受检转换。
        // 真正的精确匹配交给 getMenuType(),所以这个宽泛的 Class 不会误伤其它 Mekanism 机器。
        return (Class<MekanismTileContainer<TileEntityMekInfusioner>>) (Class<?>) MekanismTileContainer.class;
    }

    @Override
    public Optional<MenuType<MekanismTileContainer<TileEntityMekInfusioner>>> getMenuType() {
        return Optional.of(MekContainerTypes.MEK_INFUSIONER.get());
    }

    @Override
    public RecipeType<IInfusionRecipe> getRecipeType() {
        return InfusionCategory.RECIPE_TYPE;
    }

    @Override
    public boolean canHandle(MekanismTileContainer<TileEntityMekInfusioner> container, IInfusionRecipe recipe) {
        return true;
    }

    /**
     * 材料必须凑齐才有意义(注魔是 9 格一起判定的),所以让 JEI 在缺料时直接报错而不是填一半。
     */
    @Override
    public boolean requireCompleteSets(MekanismTileContainer<TileEntityMekInfusioner> container, IInfusionRecipe recipe) {
        return true;
    }

    @Override
    public List<Slot> getRecipeSlots(MekanismTileContainer<TileEntityMekInfusioner> container, IInfusionRecipe recipe) {
        List<Slot> recipeSlots = new ArrayList<>();
        for (IInventorySlot infusionSlot : container.getTileEntity().getInfusionSlots()) {
            Slot slot = findContainerSlot(container, infusionSlot);
            if (slot != null) {
                recipeSlots.add(slot);
            }
        }
        return recipeSlots;
    }

    @Override
    public List<Slot> getInventorySlots(MekanismTileContainer<TileEntityMekInfusioner> container, IInfusionRecipe recipe) {
        // 只从玩家背包取料。刻意不把机器自己的输入槽算进来 —— 它们和 recipeSlots 是同一批槽,
        // 混进来源列表会让 JEI 生成"槽位搬到自己"的无效点击。
        List<Slot> slots = new ArrayList<>(container.getMainInventorySlots());
        slots.addAll(container.getHotBarSlots());
        return slots;
    }

    @Nullable
    private static Slot findContainerSlot(MekanismTileContainer<?> container, IInventorySlot target) {
        for (InventoryContainerSlot slot : container.getInventoryContainerSlots()) {
            if (slot.getInventorySlot() == target) {
                return slot;
            }
        }
        return null;
    }
}
