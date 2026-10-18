package dev.efm.solaris_resources.client.gui;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;

public final class SourceStorages {

    private SourceStorages() {
    }

    /** 把 Ars Nouveau 的魔源方块实体适配成 widget 需要的两值视图。 */
    public static SourceStorage of(ISourceTile tile) {
        return new SourceStorage() {
            @Override
            public int getSource() {
                return tile.getSource();
            }

            @Override
            public int getMaxSource() {
                return tile.getMaxSource();
            }
        };
    }
}
