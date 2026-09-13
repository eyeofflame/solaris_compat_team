package dev.efm.rpg.widgets;

import com.lowdragmc.lowdraglib.LDLib;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * 读取立绘贴图的真实像素尺寸，用来保持宽高比。
 *
 * <p>为什么需要这个：LDLib 的 {@code ResourceTexture.drawInternal} 是把整张图的 UV 0..1
 * 铺满控件矩形的，也就是说控件矩形是什么比例，图就被拉成什么比例。所以矩形得自己按
 * 原图宽高比算，否则立绘会变形。
 *
 * <p>尺寸按 {@link ResourceLocation} 走资源管理器读取，因此资源包对同一路径的覆盖会自动生效。
 */
public final class PortraitTextures {

    private static final Map<String, int[]> SIZE_CACHE = new HashMap<>();

    private PortraitTextures() {
    }

    /**
     * @param location 纹理的完整资源路径，形如
     *                 {@code solaris_compat:textures/gui/portrait/olivia.png}
     *                 （Minecraft 的纹理 ResourceLocation 本身就含 {@code textures/} 前缀和
     *                 {@code .png} 后缀，不用再拼）
     * @return {@code {width, height}}；资源包没有提供这张图时返回 {@code null}
     */
    public static int[] sizeOf(ResourceLocation location) {
        String key = location.toString();
        int[] cached = SIZE_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        var resource = Minecraft.getInstance().getResourceManager().getResource(location);
        if (resource.isEmpty()) {
            // 资源包没提供这张立绘。不缓存失败结果，玩家加了资源包重载后还能再试。
            return null;
        }
        try (InputStream in = resource.get().open(); NativeImage image = NativeImage.read(in)) {
            if (image.getWidth() <= 0 || image.getHeight() <= 0) {
                return null;
            }
            int[] size = {image.getWidth(), image.getHeight()};
            SIZE_CACHE.put(key, size);
            return size;
        } catch (Exception e) {
            // 文件存在但读不出来（格式不对之类）
            LDLib.LOGGER.warn("立绘读取失败: {}", location, e);
            return null;
        }
    }

    /** 资源包重载后调用，丢掉缓存的尺寸。 */
    public static void clearCache() {
        SIZE_CACHE.clear();
    }
}
