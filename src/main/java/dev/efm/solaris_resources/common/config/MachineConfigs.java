package dev.efm.solaris_resources.common.config;

import com.mojang.logging.LogUtils;
import dev.efm.solaris_resources.SolarisResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;
import org.slf4j.Logger;

import java.util.List;

public final class MachineConfigs {

    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * 默认值刻意与原先硬编码的随机产出一致：三条分支 0-2 / 2-6 / 50-60，
     * 数量区间分别 1-2、1-5、1-9；物品留空表示"这个槽还没配产出"。
     */
    public static final MachineConfigStore MAGIC_CORE = new MachineConfigStore(
            FMLPaths.CONFIGDIR.get().resolve(SolarisResources.MODID).resolve("machine").resolve("magic_core_I.json"),
            List.of(
                    new MachineConfigStore.OutputSpec("", 1, 2, 0, 2),
                    new MachineConfigStore.OutputSpec("", 1, 5, 2, 6),
                    new MachineConfigStore.OutputSpec("", 1, 9, 50, 60)),
            id -> {
                ResourceLocation location = ResourceLocation.tryParse(id);
                return location != null && ForgeRegistries.ITEMS.containsKey(location);
            },
            (file, error) -> LOGGER.error("Failed to load machine config {}; keeping previous settings", file, error));

    private MachineConfigs() {
    }
}
