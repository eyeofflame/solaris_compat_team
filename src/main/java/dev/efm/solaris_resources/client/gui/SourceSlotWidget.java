package dev.efm.solaris_resources.client.gui;

import com.lowdragmc.lowdraglib.gui.editor.annotation.Configurable;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.ConfiguratorGroup;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.editor.configurator.WrapperConfigurator;
import com.lowdragmc.lowdraglib.gui.texture.ProgressTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.util.DrawerHelper;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.utils.Position;
import com.lowdragmc.lowdraglib.utils.Size;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.efm.solaris_resources.SolarisResources;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/**
 * 魔源槽：和流体槽一样是 18×18 的槽位，只是填充的是新生魔艺的魔源。
 * <p>
 * 注册进 LDLib 编辑器靠 {@link LDLRegister}——它由 Forge 的全量注解扫描
 * （{@code ModList.get().getAllScanData()}）收集，不限于 LDLib 自己的包，
 * 所以本模组不必 fork LDLib。编辑器只认已有的四个页签，这里归到
 * {@code widget.container}（和 fluid_slot 同一栏）。
 * <p>
 * 编辑器实例化时没有 {@link SourceStorage}，因此所有读取都必须容忍 null；
 * 真实 UI 在 {@code createUITemplate} 里用 {@link #setSourceStorage} 注入，服务端与客户端都要调用。
 */
@LDLRegister(name = "source_slot", group = "widget.container", modID = SolarisResources.MODID)
public class SourceSlotWidget extends Widget implements IConfigurableWidget {

    /** 复用流体槽的 18×18 边框，不为一个槽位单独加贴图。 */
    public static final ResourceBorderTexture SLOT_TEXTURE =
            new ResourceBorderTexture("ldlib:textures/gui/fluid_slot.png", 18, 18, 1, 1);

    private static final int SOURCE_COLOR = 0xFFA64DFF;

    /** 编辑器预览用的假数据，避免在配方/存档里凭空生成一个真实魔源容器。 */
    private static final SourceStorage PREVIEW_STORAGE = new SourceStorage() {
        @Override
        public int getSource() {
            return 300_000;
        }

        @Override
        public int getMaxSource() {
            return 1_000_000;
        }
    };

    @Configurable(name = "ldlib.gui.editor.name.showAmount")
    protected boolean showAmount = true;
    @Configurable(name = "ldlib.gui.editor.name.drawHoverOverlay")
    protected boolean drawHoverOverlay = true;
    @Configurable(name = "ldlib.gui.editor.name.drawHoverTips")
    protected boolean drawHoverTips = true;
    @Configurable(name = "ldlib.gui.editor.name.fillDirection")
    protected ProgressTexture.FillDirection fillDirection = ProgressTexture.FillDirection.DOWN_TO_UP;

    /** 运行期注入；编辑器里为 null。 */
    private SourceStorage storage;
    private int lastSource;
    private int lastMaxSource;

    public SourceSlotWidget() {
        this(null, 0, 0, 18, 18);
    }

    public SourceSlotWidget(SourceStorage storage, int x, int y) {
        this(storage, x, y, 18, 18);
    }

    public SourceSlotWidget(SourceStorage storage, int x, int y, int width, int height) {
        super(new Position(x, y), new Size(width, height));
        this.storage = storage;
        initTemplate();
        snapshot();
    }

    /** 把实时存储读进同步缓存；客户端自建 UI（没有 initial data）也靠它立刻显示。 */
    private void snapshot() {
        if (storage != null) {
            this.lastSource = storage.getSource();
            this.lastMaxSource = storage.getMaxSource();
        }
    }

    @Override
    public void initTemplate() {
        setBackground(SLOT_TEXTURE);
        this.fillDirection = ProgressTexture.FillDirection.DOWN_TO_UP;
    }

    public SourceSlotWidget setSourceStorage(SourceStorage storage) {
        this.storage = storage;
        snapshot();
        if (isClientSideWidget) {
            setClientSideWidget();
        }
        return this;
    }

    @Override
    public SourceSlotWidget setClientSideWidget() {
        super.setClientSideWidget();
        snapshot();
        return this;
    }

    public int getLastSource() {
        return lastSource;
    }

    public int getLastMaxSource() {
        return lastMaxSource;
    }

    // ---------------------------------------------------------------- 同步

    @Override
    public void writeInitialData(FriendlyByteBuf buffer) {
        buffer.writeBoolean(storage != null);
        if (storage != null) {
            this.lastSource = storage.getSource();
            this.lastMaxSource = storage.getMaxSource();
            buffer.writeVarInt(lastSource);
            buffer.writeVarInt(lastMaxSource);
        }
    }

    @Override
    public void readInitialData(FriendlyByteBuf buffer) {
        if (buffer.readBoolean()) {
            this.lastSource = buffer.readVarInt();
            this.lastMaxSource = buffer.readVarInt();
        }
    }

    @Override
    public void detectAndSendChanges() {
        if (storage == null) {
            return;
        }
        int source = storage.getSource();
        int max = storage.getMaxSource();
        if (max != lastMaxSource) {
            this.lastMaxSource = max;
            writeUpdateInfo(0, buffer -> buffer.writeVarInt(max));
        }
        if (source != lastSource) {
            this.lastSource = source;
            writeUpdateInfo(1, buffer -> buffer.writeVarInt(source));
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void readUpdateInfo(int id, FriendlyByteBuf buffer) {
        if (id == 0) {
            this.lastMaxSource = buffer.readVarInt();
        } else if (id == 1) {
            this.lastSource = buffer.readVarInt();
        } else {
            super.readUpdateInfo(id, buffer);
        }
    }

    // ---------------------------------------------------------------- 绘制

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.drawInBackground(graphics, mouseX, mouseY, partialTicks);
        Position pos = getPosition();
        Size size = getSize();

        if (lastSource > 0 && lastMaxSource > 0) {
            double progress = Math.min(1.0D, lastSource / (double) lastMaxSource);
            float drawnU = (float) fillDirection.getDrawnU(progress);
            float drawnV = (float) fillDirection.getDrawnV(progress);
            float drawnWidth = (float) fillDirection.getDrawnWidth(progress);
            float drawnHeight = (float) fillDirection.getDrawnHeight(progress);
            int innerWidth = size.width - 2;
            int innerHeight = size.height - 2;
            RenderSystem.enableBlend();
            DrawerHelper.drawSolidRect(graphics,
                    Math.round(pos.x + 1 + drawnU * innerWidth),
                    Math.round(pos.y + 1 + drawnV * innerHeight),
                    Math.round(drawnWidth * innerWidth),
                    Math.round(drawnHeight * innerHeight),
                    SOURCE_COLOR);
            RenderSystem.disableBlend();
        }

        if (showAmount && lastSource > 0) {
            graphics.pose().pushPose();
            graphics.pose().scale(0.5F, 0.5F, 1.0F);
            String text = String.valueOf(lastSource);
            Font font = Minecraft.getInstance().font;
            graphics.drawString(font, text,
                    (int) ((pos.x + size.width / 3F) * 2 - font.width(text) + 21),
                    (int) ((pos.y + size.height / 3F) * 2),
                    0xFFFFFF, true);
            graphics.pose().popPose();
        }

        drawOverlay(graphics, mouseX, mouseY, partialTicks);
        if (drawHoverOverlay && isMouseOverElement(mouseX, mouseY) && getHoverElement(mouseX, mouseY) == this) {
            RenderSystem.colorMask(true, true, true, false);
            DrawerHelper.drawSolidRect(graphics, pos.x + 1, pos.y + 1, size.width - 2, size.height - 2, 0x80FFFFFF);
            RenderSystem.colorMask(true, true, true, true);
        }
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void drawInForeground(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (drawHoverTips && isMouseOverElement(mouseX, mouseY) && getHoverElement(mouseX, mouseY) == this) {
            if (gui != null) {
                gui.getModularUIGui().setHoverTooltip(
                        List.of(Component.translatable("tooltip.solaris_resources.source_slot", lastSource, lastMaxSource)),
                        ItemStack.EMPTY, null, null);
            }
        } else {
            super.drawInForeground(graphics, mouseX, mouseY, partialTicks);
        }
    }

    // ---------------------------------------------------------------- 编辑器

    @Override
    public void buildConfigurator(ConfiguratorGroup father) {
        SourceSlotWidget preview = new SourceSlotWidget(PREVIEW_STORAGE, 0, 0) {
            @Override
            public void updateScreen() {
                super.updateScreen();
                showAmount = SourceSlotWidget.this.showAmount;
                drawHoverOverlay = SourceSlotWidget.this.drawHoverOverlay;
                drawHoverTips = SourceSlotWidget.this.drawHoverTips;
                fillDirection = SourceSlotWidget.this.fillDirection;
                setBackground(SourceSlotWidget.this.backgroundTexture);
            }
        };
        father.addConfigurators(new WrapperConfigurator("ldlib.gui.editor.group.preview", preview));
        IConfigurableWidget.super.buildConfigurator(father);
    }
}
