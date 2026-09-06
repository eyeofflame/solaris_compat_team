package com.example.galgame.client;

import com.example.galgame.api.CharacterProfile;
import com.example.galgame.api.GalGameScript;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * 打开 GalGame 对话界面的客户端入口：用 LDlib 的 {@link ModularUI} 组装界面，
 * 并通过 {@link ModularUIGuiContainer}（LDlib 内置的 Screen）在客户端显示。
 *
 * <p>使用了一个 {@code isRemote()=true} 的持器，使整棵 widget 树被视为纯客户端组件，
 * 免去服务端网络同步（符合单人剧情演出场景）。</p>
 */
public final class GalGameClient {

    /** 纯客户端 holder：报告 isRemote=true，使 widget 免网络同步。 */
    private static final IUIHolder CLIENT_HOLDER = new IUIHolder() {
        @Override public ModularUI createUI(Player entityPlayer) { return null; }
        @Override public boolean isInvalid() { return false; }
        @Override public boolean isRemote() { return true; }
        @Override public void markAsDirty() { }
    };

    private GalGameClient() { }

    /**
     * 在客户端打开一段对白演出。
     *
     * @param script      剧本（由 {@link GalGameScript.Builder} 构建）
     * @param characters  角色注册表（speakerId -&gt; 角色元数据）
     * @param onFinished  剧本结束回调（可为 null）
     */
    @OnlyIn(Dist.CLIENT)
    public static void open(GalGameScript script,
                            Map<String, CharacterProfile> characters,
                            @Nullable Runnable onFinished) {
        Minecraft mc = Minecraft.getInstance();
        Player player = mc.player;
        if (player == null) return; // 尚未进入世界，安全退出

        GalGameDialogWidget engine = new GalGameDialogWidget(characters, onFinished);
        ModularUI ui = new ModularUI(
                GalGameDialogWidget.UI_W, GalGameDialogWidget.UI_H, CLIENT_HOLDER, player);
        ui.widget(engine);   // 挂到主 WidgetGroup
        ui.initWidgets();    // 初始化整棵 widget 树
        engine.load(script); // 开演

        mc.setScreen(new ModularUIGuiContainer(ui, 0));
    }
}
