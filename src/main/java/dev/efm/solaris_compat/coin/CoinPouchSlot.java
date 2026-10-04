package dev.efm.solaris_compat.coin;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 库存格的包装：把容量判据放宽到 int 上限。
 *
 * <p>原版 {@code safeInsert} 用 {@code getMaxStackSize(stack)} 判断还能不能放入，显示为 64
 * 的格子会被当成满格——不覆写就没法继续存更多硬币。另外把 {@link #setChanged()} 接到容器的
 * 差值同步上：LDLib 的 Shift 合并会原地修改 {@code getItem()} 返回的对象后只调这个钩子。
 *
 * <p>注意：{@code Slot.index} 是菜单分配的下标，不是容器下标，所以档位自己存一份。
 */
public class CoinPouchSlot extends Slot {

    private final int tier;

    public CoinPouchSlot(Container container, int tier) {
        super(container, tier, 0, 0);
        this.tier = tier;
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return container.canPlaceItem(tier, stack);
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void setChanged() {
        if (container instanceof CoinPouchContainer pouch) {
            pouch.syncLive(tier);
        }
        super.setChanged();
    }
}
