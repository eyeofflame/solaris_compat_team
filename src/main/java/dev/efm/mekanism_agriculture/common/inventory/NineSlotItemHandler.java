package dev.efm.mekanism_agriculture.common.inventory;

import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.inventory.IInventorySlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

/**
 * 把机器的注魔输入槽包装成<b>恰好 9 格</b>的 {@link IItemHandler},专供神秘农业注魔配方匹配。
 *
 * <p>神秘农业的匹配走 Forge 的 {@code RecipeMatcher.findMatches},它要求传入的物品数量与配方的
 * ingredient 数量<b>严格相等</b>({@code InfusionRecipe} 恒为 9)。而 {@code ISpecialRecipe#matches}
 * 的默认实现用 {@code handler.getSlots()} 当长度,所以若图省事把机器自身的物品 capability 传进去
 * (9 输入 + 输出 + 能量 = 11 格),长度对不上会<b>静默地永远不匹配</b>——不报错,机器就是不动。
 * 因此必须用这个只暴露输入槽的视图。
 */
public final class NineSlotItemHandler implements IItemHandler {

    private final List<IInventorySlot> slots;

    public NineSlotItemHandler(List<IInventorySlot> slots) {
        this.slots = List.copyOf(slots);
    }

    @Override
    public int getSlots() {
        return slots.size();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return slots.get(slot).getStack();
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return slots.get(slot).insertItem(stack, Action.get(!simulate), AutomationType.EXTERNAL);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return slots.get(slot).extractItem(amount, Action.get(!simulate), AutomationType.EXTERNAL);
    }

    @Override
    public int getSlotLimit(int slot) {
        return slots.get(slot).getLimit(ItemStack.EMPTY);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return slots.get(slot).isItemValid(stack);
    }
}
