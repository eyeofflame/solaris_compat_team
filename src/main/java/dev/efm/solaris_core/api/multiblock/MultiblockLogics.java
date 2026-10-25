package dev.efm.solaris_core.api.multiblock;

import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * 机器逻辑注册表：图纸 id -> {@link IMultiblockLogic}。
 */
public final class MultiblockLogics {

    private static final Map<ResourceLocation, IMultiblockLogic> LOGICS = new HashMap<>();

    private MultiblockLogics() {
    }

    public static void register(ResourceLocation multiblockId, IMultiblockLogic logic) {
        LOGICS.put(multiblockId, logic);
    }

    /** 未注册时返回 {@code null}。 */
    public static IMultiblockLogic get(ResourceLocation multiblockId) {
        return multiblockId == null ? null : LOGICS.get(multiblockId);
    }

    public static void clear() {
        LOGICS.clear();
    }
}
