package dev.efm.solaris_core.functions.backToDeath.menuHelper;

import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.TextTextureWidget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import dev.efm.solaris_core.SolarisCore;
import dev.efm.solaris_core.common.network.PacketHandler;
import dev.efm.solaris_core.common.network.UIPacket;
import dev.efm.solaris_core.functions.backToDeath.PlayerPositionData;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.util.List;

public class SMenuAPI {
    public static WidgetGroup createDListGUI(int guiWidth, int guiHeight, double scale, List<PlayerPositionData> dataList) {
        double times = scale / 2;

        //WidgetGroup root0 = new WidgetGroup(Position.ORIGIN, new Size(0, 0));
        WidgetGroup root = new WidgetGroup(Position.ORIGIN, new Size((int) (220 * times), (int) (140 * times)));
        //root0.addWidget(root);
        //root.setSize((int) (220 * times), (int) (140 * times));
        root.setBackground(ResourceBorderTexture.BORDERED_BACKGROUND);
        root.setId(SolarisCore.MODID + ":DListGUI");

        int x = (guiWidth / 2 - root.getSize().width) / 2;
        int y = (guiHeight / 2 - root.getSize().height) / 2;

        DraggableScrollableWidgetGroup group = new DraggableScrollableWidgetGroup();
        group.setDraggable(false);
        group.setScrollable(true);
        group.setSize((int) (200 * times), (int) (100 * times));
        group.setSelfPosition(10, 30);

        if (!dataList.isEmpty()) for (int i = 0; i < dataList.size(); i++) {
            PlayerPositionData data = dataList.get(i);

            var text = new TextTexture(String.format("%d-%s %.1f,%.1f,%.1f", i + 1, data.dimension.getPath(), data.position.x, data.position.y, data.position.z)).scale((float) times);
            SolarisButtonWidget buttonWidget = new SolarisButtonWidget();

            buttonWidget.setButtonTexture(ResourceBorderTexture.VANILLA_BUTTON_NORMAL, text);
            buttonWidget.setHoverTexture(ResourceBorderTexture.VANILLA_BUTTON_SELECTED, text);
            buttonWidget.setClickedTexture(ResourceBorderTexture.VANILLA_BUTTON_PRESSED, text);

            int finalI = i;
            Minecraft mc = Minecraft.getInstance();
            buttonWidget.setOnPressCallback(clickData -> {
                if (mc.player != null) {
                    PacketHandler.GUI_INSTANCE.sendToServer(new UIPacket(mc.player.getId(), dataList, (float) finalI));
                    mc.setScreen(null);
                }

            });

            buttonWidget.setSize((int) (195 * times), (int) (20 * times));
            buttonWidget.setSelfPositionY((int) (i * 20 * times));
            group.addWidget(i, buttonWidget);
        }

        TextTextureWidget textTextureWidget = new TextTextureWidget();
        textTextureWidget.setSize(220, 20);
        textTextureWidget.setSelfPosition(0, 10);
        textTextureWidget.textureStyle(style -> {
            style.setType(TextTexture.TextType.OVERFLOW);
            style.scale((float) times);
        });
        textTextureWidget.setLastComponent(Component.translatable("tip.solaris_core.death_gui"));
        //root.setSelfPosition(x, y);
        root.addWidgets(group, textTextureWidget);
        return root;
    }
}
