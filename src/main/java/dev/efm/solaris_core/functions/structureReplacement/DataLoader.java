package dev.efm.solaris_core.functions.structureReplacement;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import dev.efm.solaris_core.functions.mutiStructureJson.SDataConfig;
import dev.efm.solaris_core.helper.GameHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraftforge.event.AddReloadListenerEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.*;

public class DataLoader {
    public static class ReplacementDataLoader extends SimpleJsonResourceReloadListener {
        public static final ReplacementDataLoader INSTANCE = new ReplacementDataLoader();
        private Map<ResourceLocation, ResourceLocation> allReplacements = Map.of();

        public ReplacementDataLoader() {
            super(new GsonBuilder().setPrettyPrinting().create(), "structure_replacements");
        }

        @Override
        protected void apply(@NotNull Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
            Map<ResourceLocation, ResourceLocation> merged = new HashMap<>();

            for (var entry : pObject.entrySet()) {
                try {
                    JsonObject json = entry.getValue().getAsJsonObject();
                    DataConfig config = DataConfig.fromJson(json);

                    merged.putAll(config.replacements);
                } catch (Exception e) {
                    System.err.println(entry.getKey() + " -> " + e.getMessage());
                }
            }

            this.allReplacements = Collections.unmodifiableMap(merged);
            System.out.println("Have load " + merged.size() + " replacement rules");
        }

        public ResourceLocation getReplacement(ResourceLocation original) {
            return allReplacements.get(original);
        }

        public boolean hasReplacement(ResourceLocation original) {
            return allReplacements.containsKey(original);
        }

        public boolean isEmpty() {
            return allReplacements.isEmpty();
        }

    }

    public static class MultiStructure extends SimpleJsonResourceReloadListener {
        private static final Logger LOGGER = LogUtils.getLogger();
        private final Map<ResourceLocation, SDataConfig.WithCore> withCore = new HashMap<>();
        private final Map<ResourceLocation, SDataConfig.NoCore> noCore = new HashMap<>();
        public static final MultiStructure INSTANCE = new MultiStructure();

        public MultiStructure() {
            super(new GsonBuilder().create(), "multi_structures");
        }

        public Optional<SDataConfig.WithCore> getWithCore(ResourceLocation id) {
            return Optional.ofNullable(withCore.get(id));
        }

        public Optional<SDataConfig.NoCore> getNoCore(ResourceLocation id) {
            return Optional.ofNullable(noCore.get(id));
        }

        public Collection<SDataConfig.WithCore> allWithCore() {
            return Collections.unmodifiableCollection(withCore.values());
        }

        public Collection<SDataConfig.NoCore> allNoCore() {
            return Collections.unmodifiableCollection(noCore.values());
        }

        @Override
        protected void apply(@NotNull Map<ResourceLocation, JsonElement> pObject, @NotNull ResourceManager pResourceManager, @NotNull ProfilerFiller pProfiler) {
            withCore.clear();
            noCore.clear();

            for (var entry : pObject.entrySet()) {
                ResourceLocation fileId = entry.getKey();
                String path = fileId.getPath();

                if (path.startsWith("with_core/")) {
                    parseAndStore(entry.getValue(),fileId,path.substring("with_core/".length()),SDataConfig.WithCore.CODEC,withCore);
                }else if (path.startsWith("no_core/")){
                    parseAndStore(entry.getValue(),fileId,path.substring("no_core/".length()),SDataConfig.NoCore.CODEC,noCore);
                }else {
                    LOGGER.warn("Skipping {} - not in with_core/ or no_core/",fileId);
                }
            }
        }

        private <T> void parseAndStore(JsonElement json, ResourceLocation fileId, String fileName, Codec<T> codec, Map<ResourceLocation, T> target) {
            if (fileName.endsWith(".json")) {
                fileName = fileName.substring(0, fileName.length() - 5);
            }

            String finalFileName = fileName;
            codec.parse(JsonOps.INSTANCE, json)
                    .resultOrPartial(err -> LOGGER.error("Failed to parse {} : {}", fileId, err))
                    .ifPresent(parsed -> {
                        ResourceLocation id = GameHelper.buildRes(fileId.getNamespace(), finalFileName);
                        target.put(id, parsed);
                        LOGGER.debug("Loaded {} -> {}", fileId, id);
                    });
        }
    }

    //events
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(ReplacementDataLoader.INSTANCE);
        event.addListener(MultiStructure.INSTANCE);
    }
}