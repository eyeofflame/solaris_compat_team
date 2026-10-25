package dev.efm.solaris_core.api.multiblock;

import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 图纸注册表：id -> {@link Multiblock}。
 * <p>
 * 右键触发时遍历这里的所有图纸，先做 trigger 方块预检，再逐格比对模板。
 */
public final class MultiblockRegistry {

    private static final Map<ResourceLocation, Multiblock> REGISTRY = new LinkedHashMap<>();

    private MultiblockRegistry() {
    }

    /** 注册并返回同一个实例，方便写成 {@code public static final Multiblock X = MultiblockRegistry.register(...)}。 */
    public static Multiblock register(Multiblock multiblock) {
        Objects.requireNonNull(multiblock, "multiblock");
        Multiblock previous = REGISTRY.putIfAbsent(multiblock.getId(), multiblock);
        if (previous != null && previous != multiblock) {
            throw new IllegalStateException("Duplicate multiblock id: " + multiblock.getId());
        }
        return multiblock;
    }

    /** 未注册时返回 {@code null}。 */
    public static Multiblock get(ResourceLocation id) {
        return REGISTRY.get(id);
    }

    public static boolean contains(ResourceLocation id) {
        return REGISTRY.containsKey(id);
    }

    public static Collection<Multiblock> all() {
        return REGISTRY.values();
    }

    /** 仅供测试/数据重载使用。 */
    public static void clear() {
        REGISTRY.clear();
    }
}
