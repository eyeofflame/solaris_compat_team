package dev.efm.solaris_compat.events;

import dev.efm.solaris_compat.coin.CoinPouchData;
import dev.efm.solaris_compat.coin.CoinPouchService;
import dev.efm.solaris_compat.coin.CoinTier;
import dev.efm.solaris_compat.config.SolarisConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/**
 * 自动收入：装备着硬币袋的玩家，背包里出现的硬币/硬币堆会被自动收进袋里
 * （硬币堆按 1 堆 = 9 枚换算）。
 *
 * <p>检测方式是**服务端每 tick 扫一遍背包**：Forge 没有「物品进入背包」的统一事件，
 * 拾取/合成/指令/开箱/别的 mod 管道各走各的，逐事件挂钩必漏；扫背包慢一 tick 但全覆盖。
 * 每 tick 的是 41 个格子，开销可忽略。
 *
 * <p>注意是「装备着就全收」而不是「只收新获得的」——刚装备上时背包里已有的硬币也会被收进去。
 * 开关见 {@link SolarisConfig#CoinPouchAutoCollect}。
 */
public final class CoinAutoCollectHandler {

    private CoinAutoCollectHandler() {
    }

    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }
        if (!SolarisConfig.CoinPouchAutoCollect.get()) {
            return;
        }
        absorbPlayerCoins(player);
    }

    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        // 清掉界面容器的登记，避免一直持有玩家对象
        CoinPouchService.untrackOpenUI(event.getEntity());
    }

    private static void absorbPlayerCoins(ServerPlayer player) {
        ItemStack pouch = CoinPouchService.equippedPouch(player);
        if (pouch.isEmpty()) {
            return;
        }
        Inventory inventory = player.getInventory();
        boolean changed = false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            int tier = CoinTier.tierOf(stack);
            if (tier < 0) {
                continue;
            }
            CoinPouchData.add(pouch, tier, CoinTier.coinsFor(stack));
            // 换新对象清空，避免调用方还握着原来的栈
            inventory.setItem(i, ItemStack.EMPTY);
            changed = true;
        }
        if (changed) {
            inventory.setChanged();
            // 界面开着的话让显示格跟上（标签由 LDLib 自己轮询推送）
            CoinPouchService.refreshOpenUI(player);
        }
    }
}
