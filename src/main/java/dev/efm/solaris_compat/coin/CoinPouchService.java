package dev.efm.solaris_compat.coin;

import dev.efm.solaris_compat.common.SRegistry;
import dev.efm.solaris_compat.ldlib.CoinPouchFactory;
import dev.efm.solaris_compat.ldlib.CoinPouchHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 硬币袋的服务端逻辑入口：开界面、校验、整档取出、自动收入。 */
public final class CoinPouchService {

    /** 单次发放/取回时的分堆大小，与硬币物品的最大堆叠一致。 */
    private static final int BATCH = 64;

    /**
     * 正开着的硬币袋界面（玩家 UUID → 界面里的容器）。
     *
     * <p>外部改 NBT（自动收入、别处写入）时界面里的显示格不会自己刷新，需要拿到容器重建；
     * 退出登录时清掉，避免长期持有玩家对象。用并发表是因为客户端也可能调（实际只服务端注册）。
     */
    private static final Map<UUID, CoinPouchContainer> OPEN_POUCHES = new ConcurrentHashMap<>();

    private CoinPouchService() {
    }

    public static boolean isCoinPouch(ItemStack stack) {
        return !stack.isEmpty() && stack.is(SRegistry.COIN_POUCH.get());
    }

    /** 玩家装备在硬币袋槽位里的袋子；没装返回空栈。 */
    public static ItemStack equippedPouch(Player player) {
        ItemStack stack = CuriosApi.getCuriosInventory(player).resolve()
                .flatMap(handler -> handler.getStacksHandler(CoinPouchHolder.SLOT_ID))
                .map(handler -> handler.getStacks().getStackInSlot(0))
                .orElse(ItemStack.EMPTY);
        return isCoinPouch(stack) ? stack : ItemStack.EMPTY;
    }

    /** 玩家是否在硬币袋槽位装着硬币袋（快捷键 C2S 包的服务端复核）。 */
    public static boolean hasEquipped(Player player) {
        return !equippedPouch(player).isEmpty();
    }

    public static void trackOpenUI(Player player, CoinPouchContainer container) {
        if (!player.level().isClientSide()) {
            OPEN_POUCHES.put(player.getUUID(), container);
        }
    }

    public static void untrackOpenUI(Player player) {
        OPEN_POUCHES.remove(player.getUUID());
    }

    /** 外部改了硬币袋 NBT 后，让正开着的界面重建显示格（没开着就是空转）。 */
    public static void refreshOpenUI(Player player) {
        CoinPouchContainer container = OPEN_POUCHES.get(player.getUUID());
        if (container != null) {
            container.rebuildAll();
        }
    }

    public static void open(ServerPlayer player, CoinPouchHolder.Source source) {
        CoinPouchFactory.INSTANCE.openUI(new CoinPouchHolder(source), player);
    }

    /** 已装备硬币袋里某档位的余额；没装备返回 0。 */
    public static int balance(Player player, CoinTier tier) {
        ItemStack pouch = equippedPouch(player);
        return pouch.isEmpty() ? 0 : CoinPouchData.getCount(pouch, tier.ordinal());
    }

    /** 从已装备的硬币袋里扣 amount 枚（不足则扣光）；返回实际扣掉的数量。 */
    public static int spend(Player player, CoinTier tier, int amount) {
        ItemStack pouch = equippedPouch(player);
        if (pouch.isEmpty() || amount <= 0) {
            return 0;
        }
        int take = Math.min(amount, CoinPouchData.getCount(pouch, tier.ordinal()));
        if (take <= 0) {
            return 0;
        }
        CoinPouchData.add(pouch, tier.ordinal(), -take);
        refreshOpenUI(player);
        return take;
    }

    /** 把物品安全还给玩家：先塞背包，塞不下再掉在脚边（只走服务端）。 */
    public static void returnToPlayer(Player player, ItemStack stack) {
        if (stack.isEmpty() || player.level().isClientSide()) {
            return;
        }
        ItemStack copy = stack.copy();
        player.getInventory().add(copy);
        if (!copy.isEmpty()) {
            player.drop(copy, false);
        }
    }

    /** 把某一档位的库存全部取回：按 64 拆分进背包，塞不下的掉在脚边。 */
    public static void withdrawAll(ServerPlayer player, CoinPouchHolder holder, int tier) {
        ItemStack pouch = holder.resolve(player);
        if (!isCoinPouch(pouch)) {
            return;
        }
        int stored = CoinPouchData.getCount(pouch, tier);
        Item coin = CoinTier.byIndex(tier).coinItem();
        if (stored <= 0 || coin == null) {
            return;
        }

        CoinPouchData.setCount(pouch, tier, 0);
        int remaining = stored;
        while (remaining > 0) {
            int amount = Math.min(remaining, BATCH);
            ItemStack stack = new ItemStack(coin, amount);
            player.getInventory().add(stack);
            if (!stack.isEmpty()) {
                player.drop(stack, false);
            }
            remaining -= amount;
        }
    }
}
