package dev.efm.solaris_compat.ldlib;

import dev.efm.solaris_compat.coin.CoinPouchService;
import dev.efm.solaris_compat.coin.CoinTier;
import dev.efm.solaris_compat.data.TradeData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务端结算：玩家在交易界面点“交易”按钮时，从背包扣 need、发放 sell。
 * 全部校验都在服务端（客户端只是把点击发过来），材料不足则整体不执行。
 */
public final class TradeExecutor {

    private TradeExecutor() {
    }

    /**
     * @param bulk Shift+点击：按产物的一组连续结算（最多把产物买到一组的上限），材料不足就停
     */
    public static void execute(ServerPlayer player, TradeData.Level level, int index, boolean bulk) {
        List<TradeData.Trade> pool = level.getPool();
        if (index < 0 || index >= pool.size()) {
            return;
        }
        TradeData.Trade trade = pool.get(index);

        int times = 1;
        if (bulk) {
            int stackSize = Math.max(1, trade.getSell().toStack().getMaxStackSize());
            times = Math.max(1, stackSize / Math.max(1, trade.getSell().getCount()));
        }

        for (int i = 0; i < times; i++) {
            if (!tradeOnce(player, trade)) {
                break;
            }
        }
        player.containerMenu.broadcastChanges();
    }

    private static boolean tradeOnce(ServerPlayer player, TradeData.Trade trade) {
        Inventory inventory = player.getInventory();
        // 同一物品可能有多条需求：先合并再校验/扣除，否则每条都会对着同一份余额重复放行、扣款时静默少扣
        Map<NeedKey, Integer> required = new LinkedHashMap<>();
        Map<NeedKey, TradeData.TradeItem> samples = new LinkedHashMap<>();
        for (TradeData.TradeItem need : trade.getNeed()) {
            NeedKey key = new NeedKey(need.getItem(), need.getNbt());
            required.merge(key, need.getCount(), Integer::sum);
            samples.putIfAbsent(key, need);
        }

        for (Map.Entry<NeedKey, Integer> entry : required.entrySet()) {
            TradeData.TradeItem need = samples.get(entry.getKey());
            int owned = count(inventory, need);
            CoinTier tier = pouchTierOf(need);
            if (tier != null) {
                owned += CoinPouchService.balance(player, tier);
            }
            if (owned < entry.getValue()) {
                player.displayClientMessage(Component.literal("材料不足"), true);
                return false;
            }
        }

        for (Map.Entry<NeedKey, Integer> entry : required.entrySet()) {
            TradeData.TradeItem need = samples.get(entry.getKey());
            // 硬币需求优先从硬币袋扣，不够的部分再从背包扣
            int remaining = entry.getValue();
            CoinTier tier = pouchTierOf(need);
            if (tier != null) {
                remaining -= CoinPouchService.spend(player, tier, remaining);
            }
            if (remaining > 0) {
                consume(inventory, need, remaining);
            }
        }

        ItemStack result = trade.getSell().toStack();
        if (!inventory.add(result)) {
            player.drop(result, false);
        }
        grantXp(player, trade.getXp());
        return true;
    }

    /** 合并同物品需求用的键：物品 + NBT（NBT 留空 = 不挑 NBT）。 */
    private record NeedKey(Item item, CompoundTag nbt) {
    }

    /**
     * 该需求能不能走硬币袋：是「纯粹的硬币」（本档位硬币、没指定 NBT）才走，返回档位；否则 null。
     * 硬币堆不折算——袋子存的是计数，堆另有烧炼/升档用途，按整物品交易更可预期。
     */
    private static CoinTier pouchTierOf(TradeData.TradeItem need) {
        if (!need.getNbt().isEmpty()) {
            return null;
        }
        ItemStack sample = need.toStack();
        int tier = CoinTier.tierOf(sample);
        if (tier < 0 || !CoinTier.byIndex(tier).isCoin(sample)) {
            return null;
        }
        return CoinTier.byIndex(tier);
    }

    /**
     * 交易成功给玩家的经验：拆成 1~3 颗经验球，总点数等于配置的 xp。
     */
    private static void grantXp(ServerPlayer player, int xp) {
        if (xp <= 0) {
            return;
        }
        RandomSource random = player.getRandom();
        ServerLevel level = player.serverLevel();
        int orbs = Math.min(xp, 1 + random.nextInt(3));
        int remaining = xp;
        for (int i = 0; i < orbs; i++) {
            int value = i == orbs - 1 ? remaining : 1 + random.nextInt(remaining - (orbs - 1 - i));
            remaining -= value;
            level.addFreshEntity(new ExperienceOrb(level,
                    player.getX() + (random.nextDouble() - 0.5) * 0.6,
                    player.getY() + 0.5,
                    player.getZ() + (random.nextDouble() - 0.5) * 0.6,
                    value));
        }
    }

    private static boolean matches(ItemStack stack, TradeData.TradeItem need) {
        if (stack.isEmpty() || !stack.is(need.getItem())) {
            return false;
        }
        // nbt 留空表示不挑 NBT
        return need.getNbt().isEmpty() || ItemStack.isSameItemSameTags(stack, need.toStack());
    }

    private static int count(Inventory inventory, TradeData.TradeItem need) {
        int total = 0;
        for (ItemStack stack : inventory.items) {
            if (matches(stack, need)) {
                total += stack.getCount();
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (matches(stack, need)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private static void consume(Inventory inventory, TradeData.TradeItem need, int amount) {
        int remaining = amount;
        for (ItemStack stack : inventory.items) {
            if (remaining <= 0) {
                return;
            }
            if (matches(stack, need)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
        for (ItemStack stack : inventory.offhand) {
            if (remaining <= 0) {
                return;
            }
            if (matches(stack, need)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
            }
        }
    }
}
