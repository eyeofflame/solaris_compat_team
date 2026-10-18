package dev.efm.solaris_resources.client.gui;

/**
 * 魔源槽读取的最小契约：只要求"当前量"和"上限"两个值。
 * <p>
 * 方块实体可以直接实现它；新生魔艺的 {@link com.hollingsworth.arsnouveau.api.source.ISourceTile}
 * 用 {@link SourceStorages#of} 适配，不必让 widget 依赖 Ars Nouveau 的具体实现。
 */
public interface SourceStorage {
    int getSource();

    int getMaxSource();
}
