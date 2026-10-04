package dev.efm.solaris_compat.coin;

import dev.efm.solaris_compat.ldlib.CoinPouchHolder;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 硬币袋的 5 个虚拟库存格（每档位一格）。
 *
 * <p>权威数据始终是硬币袋物品 NBT 里的计数，格子显示量封顶 64：这样所有原版交互路径
 * 拿到的都是 ≤64 的正常堆叠，超量数字不会以 {@code count>64} 的 ItemStack 形式流进玩家
 * 背包（其它 mod 处理超量堆叠时可能出复制/异常）。为此这里维护一份「活对象」
 * {@code live[i]}：{@link #getItem(int)} 返回它本身，原版在原地 split/grow 后调
 * {@link #setChanged()} 或 {@link #setItem}，我们把「显示量相对上次同步的差值」折回 NBT。
 *
 * <p>客户端实例是纯镜像：不写 NBT（权威值由菜单/Curios 同步覆盖），只保证点击预测可跑。
 *
 * <p>物品对象不要缓存：Curios/原版同步会整只替换 ItemStack，每次操作都经
 * {@link #pouch()} 现取。
 */
public class CoinPouchContainer implements Container {

    /** 单格显示上限，等于硬币物品自身的最大堆叠。 */
    private static final int DISPLAY_CAP = 64;

    private final CoinPouchHolder holder;
    private final Player player;
    private final ItemStack[] live;
    private final int[] mirror;

    public CoinPouchContainer(CoinPouchHolder holder, Player player) {
        this.holder = holder;
        this.player = player;
        this.live = new ItemStack[CoinTier.VALUES.length];
        this.mirror = new int[CoinTier.VALUES.length];
        for (int i = 0; i < live.length; i++) {
            live[i] = ItemStack.EMPTY;
        }
        for (int i = 0; i < live.length; i++) {
            rebuildLive(i);
        }
    }

    /** 当前硬币袋（每次现取，见类注释）。 */
    public ItemStack pouch() {
        return holder.pouch();
    }

    public Player player() {
        return player;
    }

    private boolean clientMirror() {
        return player.level().isClientSide();
    }

    private static int countOf(ItemStack stack) {
        return stack.isEmpty() ? 0 : stack.getCount();
    }

    /** 重建全部显示格（外部改了 NBT 时用，比如装备状态下的自动收入）。 */
    public void rebuildAll() {
        for (int i = 0; i < live.length; i++) {
            rebuildLive(i);
        }
    }

    /** 按当前 NBT 计数重建显示对象（永远是**新对象**——旧对象可能已被原版塞进快捷栏）。 */
    public void rebuildLive(int tier) {
        if (tier < 0 || tier >= live.length) {
            return;
        }
        ItemStack pouch = pouch();
        int stored = pouch.isEmpty() ? 0 : CoinPouchData.getCount(pouch, tier);
        int display = Math.min(stored, DISPLAY_CAP);
        live[tier] = display <= 0 ? ItemStack.EMPTY : CoinTier.byIndex(tier).coinStack(display);
        mirror[tier] = display;
    }

    /**
     * 把活对象上的原地改动（原版 safeInsert 的 grow、LDLib Shift 的 split）折算回 NBT。
     * 客户端只更新镜像值，不落库。
     */
    public void syncLive(int tier) {
        if (tier < 0 || tier >= live.length) {
            return;
        }
        int display = countOf(live[tier]);
        if (display == mirror[tier]) {
            return;
        }
        if (clientMirror()) {
            mirror[tier] = display;
            return;
        }
        applyDelta(tier, display - mirror[tier]);
    }

    private void applyDelta(int tier, int delta) {
        if (delta == 0) {
            return;
        }
        ItemStack pouch = pouch();
        if (!pouch.isEmpty()) {
            CoinPouchData.add(pouch, tier, delta);
        }
        rebuildLive(tier);
    }

    /** 投入格换算出的硬币入账（只走服务端）。 */
    public void addToTier(int tier, int coins) {
        if (clientMirror() || coins <= 0) {
            return;
        }
        ItemStack pouch = pouch();
        if (pouch.isEmpty()) {
            return;
        }
        CoinPouchData.add(pouch, tier, coins);
        rebuildLive(tier);
    }

    // ---- Container ----

    @Override
    public int getContainerSize() {
        return live.length;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : live) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int tier) {
        return tier >= 0 && tier < live.length ? live[tier] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int tier, int amount) {
        if (amount <= 0 || tier < 0 || tier >= live.length) {
            return ItemStack.EMPTY;
        }
        if (clientMirror()) {
            int current = countOf(live[tier]);
            int take = Math.min(amount, current);
            if (take <= 0) {
                return ItemStack.EMPTY;
            }
            int left = current - take;
            live[tier] = left <= 0 ? ItemStack.EMPTY : live[tier].copyWithCount(left);
            mirror[tier] = left;
            return CoinTier.byIndex(tier).coinStack(take);
        }
        ItemStack pouch = pouch();
        if (pouch.isEmpty()) {
            return ItemStack.EMPTY;
        }
        int stored = CoinPouchData.getCount(pouch, tier);
        int take = Math.min(amount, stored);
        if (take <= 0) {
            return ItemStack.EMPTY;
        }
        CoinPouchData.setCount(pouch, tier, stored - take);
        rebuildLive(tier);
        return CoinTier.byIndex(tier).coinStack(take);
    }

    @Override
    public ItemStack removeItemNoUpdate(int tier) {
        return removeItem(tier, Integer.MAX_VALUE);
    }

    @Override
    public void setItem(int tier, ItemStack stack) {
        if (tier < 0 || tier >= live.length) {
            return;
        }
        if (clientMirror()) {
            live[tier] = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            mirror[tier] = countOf(live[tier]);
            return;
        }
        if (stack == live[tier]) {
            // 原版 safeInsert 的合并路径：先把活对象原地 grow 过，再把同一个对象交回来
            syncLive(tier);
            return;
        }
        if (stack.isEmpty()) {
            if (live[tier].isEmpty()) {
                // 调用方已把活对象 split 成空：差值就是被拿走的量
                syncLive(tier);
            } else {
                // 活对象被整体搬走（数字键交换等）：按显示量扣减
                applyDelta(tier, -countOf(live[tier]));
            }
            return;
        }
        // 外部存入（可能是与显示量合并后的栈）：先折进遗留改动，再按差值落库
        if (pouch().isEmpty()) {
            // 硬币袋已被移走：不能把玩家刚放下的东西吞掉
            CoinPouchService.returnToPlayer(player, stack);
            return;
        }
        syncLive(tier);
        applyDelta(tier, countOf(stack) - countOf(live[tier]));
    }

    @Override
    public void setChanged() {
        // 计数直接写在硬币袋物品 NBT 上，由物品自身的同步链路广播，这里无需额外处理
    }

    @Override
    public boolean stillValid(Player player) {
        return !pouch().isEmpty();
    }

    @Override
    public boolean canPlaceItem(int tier, ItemStack stack) {
        return stack.isEmpty() || CoinTier.byIndex(tier).isCoin(stack);
    }

    @Override
    public void clearContent() {
        // 硬币袋是持久存储：任何「清空容器」的调用都不能把玩家的硬币清掉
    }
}
