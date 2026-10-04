package dev.efm.solaris_compat.client;

import dev.efm.solaris_compat.SolarisCompat;
import dev.efm.solaris_compat.network.SolaNetwork;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 快捷键按下 → 发 C2S 请求打开已装备的硬币袋。
 *
 * <p>客户端不做“装没装”的判断（Curios capability 的时序不值得赌），一律交给服务端复核；
 * 只在没有界面打开、且不是长按重复时发。
 */
@Mod.EventBusSubscriber(modid = SolarisCompat.MODID, value = Dist.CLIENT)
public final class CoinPouchKeyHandler {

    private CoinPouchKeyHandler() {
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != org.lwjgl.glfw.GLFW.GLFW_PRESS || Minecraft.getInstance().screen != null) {
            return;
        }
        while (CoinPouchKeys.OPEN_COIN_POUCH.consumeClick()) {
            SolaNetwork.sendOpenCoinPouch();
        }
    }
}
