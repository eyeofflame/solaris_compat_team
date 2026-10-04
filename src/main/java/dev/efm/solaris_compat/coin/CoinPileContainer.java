package dev.efm.solaris_compat.coin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 每行一个的「投入格」：接收本档位的硬币或硬币堆，在 {@link #setItem} 里即时换算入账
 * （硬币 +1/枚、硬币堆 +9/枚），自身永不持有物品（{@link #getItem} 恒空）。
 *
 * <p>这样点击 / 拖拽 / Shift 点击 / 数字键交换所有原版路径都会汇聚到 setItem，不需要 tick
 * 扫描；且因为格子恒空，原版放置逻辑里「把旧显示量交给调用方」的那条合并分支永远不会走到。
 */
public class CoinPileContainer implements Container {

    private final CoinPouchContainer storage;
    private final int tier;

    public CoinPileContainer(CoinPouchContainer storage, int tier) {
        this.storage = storage;
        this.tier = tier;
    }

    @Override
    public int getContainerSize() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int amount) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CoinTier coinTier = CoinTier.byIndex(tier);
        int coins;
        if (coinTier.isCoin(stack)) {
            coins = stack.getCount();
        } else if (coinTier.isPile(stack)) {
            coins = stack.getCount() * CoinTier.COINS_PER_PILE;
        } else {
            // canPlaceItem 之外的兜底（正常交互不该走到）：原样还给玩家，不能凭空消失
            CoinPouchService.returnToPlayer(storage.player(), stack);
            return;
        }
        storage.addToTier(tier, coins);
    }

    @Override
    public void setChanged() {
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        CoinTier coinTier = CoinTier.byIndex(tier);
        return stack.isEmpty() || coinTier.isCoin(stack) || coinTier.isPile(stack);
    }

    @Override
    public void clearContent() {
    }
}
