package dev.efm.solaris_compat.events;

import dev.efm.solaris_compat.Solaris_compat;
import dev.efm.solaris_compat.data.DataRegistry;
import dev.efm.solaris_compat.data.pools.CommonPool;
import dev.efm.solaris_compat.data.pools.EpicPool;
import dev.efm.solaris_compat.data.pools.RarePool;
import dev.efm.solaris_compat.data.pools.UncommonPool;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = Solaris_compat.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BountyCache implements ResourceManagerReloadListener {
    private static List<CommonPool> cacheCommonPools = new ArrayList<>();
    private static List<UncommonPool> cacheUncommonPools = new ArrayList<>();
    private static List<EpicPool> cacheEpicPools = new ArrayList<>();
    private static List<RarePool> cacheRarePools = new ArrayList<>();

    public static List<CommonPool> getCachePools() {
        return cacheCommonPools;
    }

    public static List<UncommonPool> getCacheUncommonPools() {
        return cacheUncommonPools;
    }

    public static List<EpicPool> getCacheEpicPools() {
        return cacheEpicPools;
    }

    public static List<RarePool> getCacheRarePools() {
        return cacheRarePools;
    }

    @Override
    public void onResourceManagerReload(@NotNull ResourceManager pResourceManager) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        RegistryAccess access = server.registryAccess();

        Registry<CommonPool> registry0 = access
                .registry(DataRegistry.COMMON_POOL_KEY)
                .orElseThrow(() -> new IllegalStateException("Bounty Pool registry not found!"));

        Registry<UncommonPool> registry1 = access
                .registry(DataRegistry.UNCOMMON_POOL_KEY)
                .orElseThrow(() -> new IllegalStateException("Bounty Pool registry not found!"));

        Registry<EpicPool> registry2 = access
                .registry(DataRegistry.EPIC_POOL_KEY)
                .orElseThrow(() -> new IllegalStateException("Bounty Pool registry not found!"));

        Registry<RarePool> registry3 = access
                .registry(DataRegistry.RARE_POOL_KEY)
                .orElseThrow(() -> new IllegalStateException("Bounty Pool registry not found!"));

        cacheCommonPools = registry0.stream().collect(Collectors.toList());
        cacheUncommonPools = registry1.stream().collect(Collectors.toList());
        cacheEpicPools = registry2.stream().collect(Collectors.toList());
        cacheRarePools = registry3.stream().collect(Collectors.toList());
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent evt) {
        evt.addListener(new BountyCache());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent evt) {
        RegistryAccess access = evt.getServer().registryAccess();

        Registry<CommonPool> registry0 = access
                .registry(DataRegistry.COMMON_POOL_KEY)
                .orElseThrow(() -> new IllegalStateException("Bounty Pool registry not found!"));

        Registry<UncommonPool> registry1 = access
                .registry(DataRegistry.UNCOMMON_POOL_KEY)
                .orElseThrow(() -> new IllegalStateException("Bounty Pool registry not found!"));

        Registry<EpicPool> registry2 = access
                .registry(DataRegistry.EPIC_POOL_KEY)
                .orElseThrow(() -> new IllegalStateException("Bounty Pool registry not found!"));

        Registry<RarePool> registry3 = access
                .registry(DataRegistry.RARE_POOL_KEY)
                .orElseThrow(() -> new IllegalStateException("Bounty Pool registry not found!"));

        cacheCommonPools = registry0.stream().collect(Collectors.toList());
        cacheUncommonPools = registry1.stream().collect(Collectors.toList());
        cacheEpicPools = registry2.stream().collect(Collectors.toList());
        cacheRarePools = registry3.stream().collect(Collectors.toList());
    }
}
