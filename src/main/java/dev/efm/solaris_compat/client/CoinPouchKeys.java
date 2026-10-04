package dev.efm.solaris_compat.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.efm.solaris_compat.SolarisCompat;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 硬币袋快捷键（默认 B，可在控制设置里改）。
 *
 * <p>整个类只在客户端加载（{@code value = Dist.CLIENT}），专用服务器不会碰到它。
 */
@Mod.EventBusSubscriber(modid = SolarisCompat.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class CoinPouchKeys {

    public static final KeyMapping OPEN_COIN_POUCH = new KeyMapping(
            "key.solaris_compat.open_coin_pouch",
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_B,
            "key.categories.solaris_compat");

    private CoinPouchKeys() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(OPEN_COIN_POUCH);
    }
}
