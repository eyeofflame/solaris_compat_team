package dev.efm.solaris_core.client.multiblock;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * 整机 GeckoLib 模型注册表（客户端）：图纸 id -> 模型/贴图/动画路径。
 * <p>
 * 路径都写完整，例如：
 * <pre>{@code
 * new MultiblockModel(
 *     new ResourceLocation("mymod", "geo/multiblock/coke_oven.geo.json"),
 *     new ResourceLocation("mymod", "textures/multiblock/coke_oven.png"),
 *     new ResourceLocation("mymod", "animations/multiblock/coke_oven.animation.json"))  // 可为 null
 * }</pre>
 * 注册时机：客户端 {@code FMLClientSetupEvent} 或更早。
 * 没注册模型的机器会走 BER 的兜底渲染（按部件偏移逐格画方块）。
 */
public final class MultiblockModels {

    /** 一个机器的 GeckoLib 资源三件套；animation 可以为 null（无动画）。 */
    public record MultiblockModel(ResourceLocation model, ResourceLocation texture, @Nullable ResourceLocation animation) {
    }

    private static final Map<ResourceLocation, MultiblockModel> MODELS = new HashMap<>();

    private MultiblockModels() {
    }

    public static void register(ResourceLocation multiblockId, MultiblockModel model) {
        MODELS.put(multiblockId, model);
    }

    @Nullable
    public static MultiblockModel get(@Nullable ResourceLocation multiblockId) {
        return multiblockId == null ? null : MODELS.get(multiblockId);
    }
}
