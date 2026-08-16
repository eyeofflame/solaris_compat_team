package dev.efm.solaris_compat.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public class KilledEntityData {
    private final String entityId;
    private final Integer count;

    public static final Codec<KilledEntityData> CODEC = RecordCodecBuilder.create((instance) ->
            instance.group(
                    Codec.STRING.fieldOf("id").forGetter(KilledEntityData::getEntityId),
                    Codec.INT.fieldOf("Count").forGetter(KilledEntityData::getCount)
            ).apply(instance, KilledEntityData::new));

    public KilledEntityData(String entityId, Integer count) {
        this.entityId = entityId;
        this.count = count;
    }

    public String getEntityId() {
        return this.entityId;
    }

    public Integer getCount() {
        return this.count;
    }
}
