package dev.efm.solaris_resources.common.config;

import com.mojang.logging.LogUtils;
import dev.efm.solaris_resources.SolarisResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

public class AtomConfigs {
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final AtomConfigStore STORE = new AtomConfigStore(
            FMLPaths.CONFIGDIR.get().resolve(SolarisResources.MODID),
            item -> {
                if (item.isEmpty()) return true;
                ResourceLocation id = ResourceLocation.tryParse(item);
                return id != null && ForgeRegistries.ITEMS.containsKey(id);
            },
            (file, error) -> LOGGER.error("Failed to load atom config {}; keeping previous settings", file, error));
}
