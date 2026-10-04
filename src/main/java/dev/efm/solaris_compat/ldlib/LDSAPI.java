package dev.efm.solaris_compat.ldlib;

import com.lowdragmc.lowdraglib.gui.editor.data.UIProject;
import com.lowdragmc.lowdraglib.gui.texture.ColorBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.TabContainer;
import com.lowdragmc.lowdraglib.gui.widget.TextBoxWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.mojang.logging.LogUtils;
import dev.efm.solaris_compat.data.TradeData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * 把编辑器导出的 {@code solaris_trade.ui} 与 {@link TradeData} 对接。
 *
 * <p>布局（每个 level 页签里各有一份）：左侧 {@code main_back} 滚动区放该等级的
 * 全部报价列表（中空边框按钮，内容是产物贴图 + 名称，点选），右侧 {@code need_item}
 * 显示需求物图标、{@code sell_item} 直接以「默认边框 + 产物贴图」为背景，
 * {@code sell_desc} 显示配置里的 desc（按本地化键解析），右下按钮执行交易。
 *
 * <p>LDLib 的 {@code createUITemplate} 两端都会执行，组件树两端必须保持一致：
 * 列表点选的界面刷新在两端同步跑（行按钮的回调两端都会被调用），
 * 而交易结算只在服务端（{@link TradeExecutor}）。
 */
public final class LDSAPI {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation LAYOUT = ResourceLocation.parse("ldlib:solaris_trade");

    private static final int ICON_SIZE = 18;
    private static final int ICON_PITCH = 20;
    private static final int NEED_COLUMNS = 4;
    private static final int ICON_Y = 5;       // 与 need_item 上边框保持 5 的间隔

    private static final int ROW_X = 5;              // 列表左右各空出 5
    private static final int ROW_Y = 7;              // 原起点再往下 5
    private static final int ROW_WIDTH = 140;
    private static final int ROW_HEIGHT = 20;
    private static final int ROW_ICON_SIZE = 16;
    private static final int ROW_TEXT_X = 22;        // 名称放在贴图右侧
    private static final int ROW_BORDER = 0xFF7A7A7A;
    private static final int ROW_BORDER_HOVER = 0xFFFFFFFF;
    private static final int ROW_BORDER_SELECTED = 0xFF55C8FF;

    private LDSAPI() {
    }

    public static WidgetGroup createUI(SolaTradeHolder holder, Player player) {
        var supplier = UIProject.loadUIFromFile(LAYOUT);
        if (supplier == null) {
            LOGGER.error("找不到交易界面布局文件: {}", LAYOUT);
            return new WidgetGroup(0, 0, 0, 0);
        }
        WidgetGroup root = supplier.get();
        int bound = 0;
        if (root.getFirstWidgetById("backg") instanceof TabContainer tabs) {
            for (var entry : tabs.tabs.entrySet()) {
                if (bindTab(holder, player, entry.getKey().getId(), entry.getValue())) {
                    bound++;
                }
            }
        }
        LOGGER.info("[{}] 交易界面绑定: profession={}, 配置等级数={}, 命中页签={}",
                player.level().isClientSide() ? "client" : "server",
                holder.tradeData().getProfession(), holder.tradeData().getTrades().size(), bound);
        return root;
    }

    /**
     * 一个页签 = 一个村民等级。绑定成功返回 true。
     */
    private static boolean bindTab(SolaTradeHolder holder, Player player, String levelId, WidgetGroup tab) {
        TradeData.Level level = holder.tradeData().getTrades().get(levelId);
        if (level == null) {
            return false;
        }

        WidgetGroup list = asGroup(tab.getFirstWidgetById("main_back"));
        WidgetGroup needGroup = asGroup(tab.getFirstWidgetById("need_item"));
        WidgetGroup sellGroup = asGroup(tab.getFirstWidgetById("sell_item"));
        TextBoxWidget sellDesc = asTextBox(tab.getFirstWidgetById("sell_desc"));
        if (list == null || needGroup == null || sellGroup == null || sellDesc == null) {
            LOGGER.warn("交易界面缺少控件，跳过页签 {}", levelId);
            return false;
        }

        // 需求图标运行时增删，放进独立的子组里，不碰文件里原有的标签
        WidgetGroup needIcons = new WidgetGroup(0, 0, needGroup.getSize().width, needGroup.getSize().height);
        needGroup.addWidget(needIcons);

        List<ButtonWidget> rows = new ArrayList<>();
        int[] selected = {0};

        List<TradeData.Trade> pool = level.getPool();
        for (int i = 0; i < pool.size(); i++) {
            int index = i;
            int rowY = ROW_Y + i * ROW_HEIGHT;
            ItemStack sellStack = pool.get(i).getSell().toStack();
            ButtonWidget row = new ButtonWidget(ROW_X, rowY, ROW_WIDTH, ROW_HEIGHT - 2, null, clickData -> {
                selected[0] = index;
                refresh(level, selected[0], needIcons, sellGroup, sellDesc, rows);
            });
            row.setHoverTexture(new ColorBorderTexture(1, ROW_BORDER_HOVER));
            rows.add(row);
            list.addWidget(row);
            // 按钮内容叠在按钮上层：产物贴图 + 名称（普通控件不拦截点击）
            list.addWidget(new ImageWidget(ROW_X + 3, rowY + 1, ROW_ICON_SIZE, ROW_ICON_SIZE, new ItemStackTexture(sellStack)));
            list.addWidget(new LabelWidget(ROW_X + ROW_TEXT_X, rowY + 5, sellStack.getHoverName().getString()));
        }
        // .ui 里存着编辑器留下的 maxHeight（5329），内容不足时列表也会滚：
        // 按实际内容重算滚动范围，行多了照样能滚
        if (list instanceof DraggableScrollableWidgetGroup scrollable) {
            scrollable.computeMax();
        }

        ButtonWidget tradeButton = findTradeButton(tab);
        if (tradeButton != null) {
            tradeButton.setOnPressCallback(clickData -> {
                // 点击回调两端都会跑：只有服务端负责结算；Shift+左键 = 一次买一组
                if (player instanceof ServerPlayer serverPlayer) {
                    TradeExecutor.execute(serverPlayer, level, selected[0], clickData.isShiftClick);
                }
            });
        }

        // 默认选中第一条，两端建树结果一致
        refresh(level, selected[0], needIcons, sellGroup, sellDesc, rows);
        return true;
    }

    /**
     * 刷新“选中报价”的明细：需求图标、产物背景图、文字、列表高亮。
     * 行按钮回调会同时在客户端与服务端调用本方法，保证两端组件树一致。
     */
    private static void refresh(TradeData.Level level, int selected, WidgetGroup needIcons, WidgetGroup sellGroup,
                                TextBoxWidget sellDesc, List<ButtonWidget> rows) {
        List<TradeData.Trade> pool = level.getPool();
        needIcons.clearAllWidgets();

        if (pool.isEmpty()) {
            sellGroup.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
            sellDesc.setContent(List.of());
        } else {
            TradeData.Trade trade = pool.get(Math.min(selected, pool.size() - 1));

            int index = 0;
            for (TradeData.TradeItem need : trade.getNeed()) {
                int col = index % NEED_COLUMNS;
                int row = index / NEED_COLUMNS;
                needIcons.addWidget(icon(need, 4 + col * ICON_PITCH, ICON_Y + row * ICON_PITCH));
                index++;
            }

            // 产物展示 = 直接改 sell_item 的背景：默认边框为底，产物贴图盖在上面
            sellGroup.setBackground(new GuiTextureGroup(
                    ResourceBorderTexture.BORDERED_BACKGROUND,
                    new ItemStackTexture(trade.getSell().toStack())));

            String desc = trade.getDesc();
            sellDesc.setContent(desc.isEmpty() ? List.of() : List.of(Component.translatable(desc).getString()));
        }

        for (int i = 0; i < rows.size(); i++) {
            rows.get(i).setButtonTexture(new ColorBorderTexture(1, i == selected ? ROW_BORDER_SELECTED : ROW_BORDER));
        }
    }

    private static ImageWidget icon(TradeData.TradeItem item, int x, int y) {
        ItemStack stack = item.toStack();
        ImageWidget widget = new ImageWidget(x, y, ICON_SIZE, ICON_SIZE, new ItemStackTexture(stack));
        widget.setHoverTooltips(stack.getHoverName());
        return widget;
    }

    /**
     * 右下角那个按钮在文件里没有 id，按类型在 {@code desc} 组里找（该组里只有这一个按钮）。
     */
    private static ButtonWidget findTradeButton(WidgetGroup tab) {
        Widget desc = tab.getFirstWidgetById("desc");
        if (desc instanceof WidgetGroup group) {
            List<ButtonWidget> buttons = group.getWidgetsByType(ButtonWidget.class);
            if (!buttons.isEmpty()) {
                return buttons.get(0);
            }
        }
        LOGGER.warn("交易界面里没有找到交易按钮");
        return null;
    }

    private static WidgetGroup asGroup(Widget widget) {
        return widget instanceof WidgetGroup group ? group : null;
    }

    private static TextBoxWidget asTextBox(Widget widget) {
        return widget instanceof TextBoxWidget textBox ? textBox : null;
    }
}
