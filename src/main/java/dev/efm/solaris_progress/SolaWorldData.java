package dev.efm.solaris_progress;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * 存档级标记：init.nbt 是否已放置，保证只放一次。
 */
public class SolaWorldData extends SavedData {
    private static final String DATA_NAME = "solaris_progress_world";

    private boolean initPlaced = false;

    public static SolaWorldData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(SolaWorldData::load, SolaWorldData::new, DATA_NAME);
    }

    public static SolaWorldData load(CompoundTag tag) {
        SolaWorldData data = new SolaWorldData();
        data.initPlaced = tag.getBoolean("initPlaced");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("initPlaced", initPlaced);
        return tag;
    }

    public boolean isInitPlaced() {
        return initPlaced;
    }

    public void setInitPlaced(boolean placed) {
        this.initPlaced = placed;
        setDirty();
    }
}
