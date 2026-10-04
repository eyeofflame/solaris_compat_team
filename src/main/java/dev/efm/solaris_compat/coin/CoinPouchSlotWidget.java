package dev.efm.solaris_compat.coin;

import com.lowdragmc.lowdraglib.gui.widget.SlotWidget;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;

/**
 * 用自定义 {@link CoinPouchSlot} 的槽位控件（库存格与投入格共用）。
 *
 * <p>必须用自定义 Slot：LDLib 默认的槽位包装只检查「能否放入」标志，
 * **不会**查 {@code Container.canPlaceItem}，用它的话投入格会接收任意物品。
 */
public class CoinPouchSlotWidget extends SlotWidget {

    public CoinPouchSlotWidget(Container container, int index, int x, int y) {
        super(container, index, x, y, true, true);
    }

    @Override
    protected Slot createSlot(Container container, int index) {
        // 本方法在 SlotWidget 构造期就会被调用，不能依赖子类字段
        return new CoinPouchSlot(container, index);
    }
}
