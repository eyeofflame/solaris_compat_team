package dev.efm.solaris_compat.ldlib;

import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.custom.PlayerInventoryWidget;
import dev.efm.solaris_compat.coin.CoinPileContainer;
import dev.efm.solaris_compat.coin.CoinPouchContainer;
import dev.efm.solaris_compat.coin.CoinPouchData;
import dev.efm.solaris_compat.coin.CoinPouchService;
import dev.efm.solaris_compat.coin.CoinPouchSlotWidget;
import dev.efm.solaris_compat.coin.CoinTier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * 硬币袋界面：纯代码搭建，不依赖编辑器导出的 .ui 文件。
 *
 * <p>每档位一行：左「投入格」（放硬币/硬币堆，换算后格子保持空）→ 库存格（显示封顶 64）
 * → 数量文字 → 「全部取出」按钮。数量文字用 {@link LabelWidget} 的 Supplier 版本，LDLib 会
 * 在服务端轮询并在变化时推给客户端，不用自己写同步；按钮回调两端都会跑，只有服务端结算。
 *
 * <p>组件树两端必须完全一致（LDLib 按子控件索引链路由点击），所以这里不做任何运行时增删。
 */
public final class CoinPouchUI {

    private static final int WIDTH = 200;
    private static final int ROW_TOP = 20;
    private static final int ROW_HEIGHT = 22;
    private static final int PLAYER_INV_HEIGHT = 86;

    private CoinPouchUI() {
    }

    public static WidgetGroup build(CoinPouchHolder holder, Player player) {
        int rows = CoinTier.VALUES.length;
        int invY = ROW_TOP + rows * ROW_HEIGHT + 6;
        WidgetGroup root = new WidgetGroup(0, 0, WIDTH, invY + PLAYER_INV_HEIGHT + 4);
        root.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        root.addWidget(new LabelWidget(8, 5, Component.translatable("solaris_compat.coin_pouch.title").getString()));

        CoinPouchContainer storage = new CoinPouchContainer(holder, player);
        // 自动收入等外部写入发生后，靠它把正开着的显示格重建（客户端是空转）
        CoinPouchService.trackOpenUI(player, storage);
        for (int tier = 0; tier < rows; tier++) {
            int y = ROW_TOP + tier * ROW_HEIGHT;
            int index = tier;

            // 投入格：收本档位硬币/硬币堆，setItem 里即时换算，格子本身恒空
            root.addWidget(new CoinPouchSlotWidget(new CoinPileContainer(storage, tier), 0, 8, y));
            // 库存格：虚拟计数槽，显示封顶 64，计数无限
            root.addWidget(new CoinPouchSlotWidget(storage, tier, 30, y));
            // 数量文字：服务端轮询、变化时推送
            root.addWidget(new LabelWidget(52, y + 5, () -> countText(holder, index)));
            // 全部取出：回调两端都会跑，只有服务端真正结算
            root.addWidget(new ButtonWidget(146, y, 48, 18,
                    new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON,
                            new TextTexture(Component.translatable("solaris_compat.coin_pouch.withdraw_all").getString())),
                    clickData -> {
                        if (player instanceof ServerPlayer serverPlayer) {
                            CoinPouchService.withdrawAll(serverPlayer, holder, index);
                            storage.rebuildLive(index);
                        }
                    }));
        }

        PlayerInventoryWidget inventory = new PlayerInventoryWidget();
        inventory.setSelfPosition(14, invY);
        root.addWidget(inventory);
        return root;
    }

    private static String countText(CoinPouchHolder holder, int tier) {
        ItemStack pouch = holder.pouch();
        int count = CoinPouchData.getCount(pouch, tier);
        return Component.translatable(CoinTier.byIndex(tier).langKey()).getString() + " ×" + count;
    }
}
