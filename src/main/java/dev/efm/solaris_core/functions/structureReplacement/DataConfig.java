package dev.efm.solaris_core.functions.structureReplacement;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

public class DataConfig {
    public final Map<ResourceLocation, ResourceLocation> replacements = new HashMap<>();

    public void add(ResourceLocation from, ResourceLocation to) {
        replacements.put(from, to);
    }

    public ResourceLocation get(ResourceLocation from) {
        return replacements.get(from);
    }

    public boolean isEmpty() {
        return replacements.isEmpty();
    }

    public static DataConfig fromJson(JsonObject object){
        DataConfig config = new DataConfig();
        JsonObject replaceObj = object.getAsJsonObject("replace");

        for (Map.Entry<String, JsonElement> entry : replaceObj.entrySet()){
            ResourceLocation from = ResourceLocation.parse(entry.getKey());
            ResourceLocation to = ResourceLocation.parse(entry.getValue().getAsString());
            config.add(from,to);
        }
        return config;
    }
}
