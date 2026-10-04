package dev.efm.solaris_compat.common.items;

import dev.efm.solaris_compat.coin.CoinPouchData;
import dev.efm.solaris_compat.coin.CoinPouchService;
import dev.efm.solaris_compat.coin.CoinTier;
import dev.efm.solaris_compat.ldlib.CoinPouchHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.List;

/**
 * 硬币袋：无限量存储 Coins 模组（coinsje）的硬币。
 *
 * <p>拿在手上右键开界面（服务端开、LDLib 两端同构建树）；装备在 Curios 的
 * {@code coin_pouch} 槽位时按快捷键（默认 B）开。物品本身只是入口，存取逻辑在
 * {@link CoinPouchService} 与 coin 包的容器里。
 *
 * <p>实现 {@link ICurioItem}（Curios 5.x 会自动挂 capability，不需要注册调用），因此
 * Curios 是硬依赖，见 mods.toml。
 */
public class CoinPouchItem extends Item implements ICurioItem {

    public CoinPouchItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            CoinPouchService.open(serverPlayer, hand == InteractionHand.MAIN_HAND
                    ? CoinPouchHolder.Source.HAND_MAIN
                    : CoinPouchHolder.Source.HAND_OFF);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.solaris.coin_pouch.desc"));
        for (CoinTier tier : CoinTier.VALUES) {
            int count = CoinPouchData.getCount(stack, tier.ordinal());
            if (count > 0) {
                tooltip.add(Component.translatable(tier.langKey()).append(" ×" + count));
            }
        }
    }
}
