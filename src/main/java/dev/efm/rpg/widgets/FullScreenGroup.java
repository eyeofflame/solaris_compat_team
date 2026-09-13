package dev.efm.rpg.widgets;

import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Consumer;

/**
 * 尺寸始终等于屏幕的容器，兼作布局钩子。
 *
 * <p>和 {@code new ModularUI(holder, player)}（fullScreen = true）配合使用。那个构造只把
 * {@code ModularUI.width/height} 同步成屏幕尺寸，<b>不会</b>改 mainGroup 的 size，所以需要这个类
 * 在 {@link #onScreenSizeUpdate} 里自己撑满。
 *
 * <p>服务端构造时尺寸是 0×0（服务端没有屏幕概念），客户端在 {@code ModularUIGuiContainer.init()}
 * 里被撑满，之后每次窗口缩放都会自动跟随。所以<b>不要在建树阶段用 {@code getSize()} 参与坐标计算</b>
 * ——两端拿到的值不一样。要用相对位置就在 {@link #onLayout} 回调里算。
 */
public class FullScreenGroup extends WidgetGroup {

    private Consumer<FullScreenGroup> layout = g -> {
    };

    public FullScreenGroup() {
        // 0×0 起步：这样 ModularUI.setFullScreen() 内部的 setSize(0, 0) 是空操作
        super(0, 0, 0, 0);
    }

    /**
     * 注册布局回调。在客户端确定屏幕尺寸后触发（打开界面时 + 每次窗口缩放时），
     * 时机早于第一帧渲染，所以不会看到控件跳动。
     */
    public FullScreenGroup onLayout(Consumer<FullScreenGroup> layout) {
        this.layout = layout;
        return this;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void onScreenSizeUpdate(int screenWidth, int screenHeight) {
        ModularUI gui = getGui();
        if (gui != null && gui.isFullScreen()) {
            setSize(screenWidth, screenHeight);
            layout.accept(this);
        }
        super.onScreenSizeUpdate(screenWidth, screenHeight);
    }
}
