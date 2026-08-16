package dev.efm.solaris_compat.data;

import dev.efm.solaris_compat.Solaris_compat;
import dev.efm.solaris_compat.api.SHelper;
import dev.efm.solaris_compat.data.pools.CommonPool;
import dev.efm.solaris_compat.data.pools.EpicPool;
import dev.efm.solaris_compat.data.pools.RarePool;
import dev.efm.solaris_compat.data.pools.UncommonPool;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.registries.DataPackRegistryEvent;

import java.util.List;
import java.util.Set;

public class DataRegistry {
    //public static final ResourceKey<Registry<BountyPool>> BOUNTY_POOL_KEY = ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(Solaris_compat.MODID, "bounty_pool"));
    public static final ResourceKey<Registry<CommonPool>> COMMON_POOL_KEY = ResourceKey.createRegistryKey(SHelper.buildRes(Solaris_compat.MODID, "common_pool"));
    public static final ResourceKey<Registry<UncommonPool>> UNCOMMON_POOL_KEY = ResourceKey.createRegistryKey(SHelper.buildRes(Solaris_compat.MODID, "uncommon_pool"));
    public static final ResourceKey<Registry<EpicPool>> EPIC_POOL_KEY = ResourceKey.createRegistryKey(SHelper.buildRes(Solaris_compat.MODID, "epic_pool"));
    public static final ResourceKey<Registry<RarePool>> RARE_POOL_KEY = ResourceKey.createRegistryKey(SHelper.buildRes(Solaris_compat.MODID, "rare_pool"));

    public static void DataRegistryEvent(DataPackRegistryEvent.NewRegistry evt) {
        //evt.dataPackRegistry(BOUNTY_POOL_KEY, BountyPool.CODEC);
        evt.dataPackRegistry(COMMON_POOL_KEY, CommonPool.CODEC);
        evt.dataPackRegistry(UNCOMMON_POOL_KEY, UncommonPool.CODEC);
        evt.dataPackRegistry(EPIC_POOL_KEY, EpicPool.CODEC);
        evt.dataPackRegistry(RARE_POOL_KEY, RarePool.CODEC);
    }

    //pool registry

    //public static final ResourceKey<BountyPool> COMMON_POOL = ResourceKey.create(BOUNTY_POOL_KEY, SHelper.buildRes(Solaris_compat.MODID, "common_pool"));
    //public static final ResourceKey<BountyPool> UNCOMMON_POOL = ResourceKey.create(BOUNTY_POOL_KEY, SHelper.buildRes(Solaris_compat.MODID, "uncommon_pool"));
    public static final ResourceKey<CommonPool> EXAMPLE_POOL = ResourceKey.create(COMMON_POOL_KEY, SHelper.buildRes(Solaris_compat.MODID, "fuck"));
    public static final ResourceKey<UncommonPool> EXAMPLE_POOL0 = ResourceKey.create(UNCOMMON_POOL_KEY, SHelper.buildRes(Solaris_compat.MODID, "fuck"));
    public static final ResourceKey<EpicPool> EXAMPLE_POOL1 = ResourceKey.create(EPIC_POOL_KEY, SHelper.buildRes(Solaris_compat.MODID, "fuck"));
    public static final ResourceKey<RarePool> EXAMPLE_POOL2 = ResourceKey.create(RARE_POOL_KEY, SHelper.buildRes(Solaris_compat.MODID, "fuck"));

    public static void GatherDataEvent(GatherDataEvent evt) {
        DataGenerator generator = evt.getGenerator();

        RegistrySetBuilder builder = new RegistrySetBuilder();

        builder.add(COMMON_POOL_KEY, boot -> {
            boot.register(EXAMPLE_POOL, new CommonPool("simple_pool", List.of(Items.STONE.getDefaultInstance()), List.of(), List.of(Items.DIAMOND.getDefaultInstance())));
        });
        builder.add(UNCOMMON_POOL_KEY, boot -> {
            boot.register(EXAMPLE_POOL0, new UncommonPool("fuck_pool", List.of(Items.STONE.getDefaultInstance()), List.of(), List.of(Items.DIAMOND.getDefaultInstance())));
        });
        builder.add(EPIC_POOL_KEY, boot -> {
            boot.register(EXAMPLE_POOL1, new EpicPool("fuck_pool", List.of(Items.STONE.getDefaultInstance()), List.of(new KilledEntityData("minecraft:zombie",3)), List.of(Items.DIAMOND.getDefaultInstance())));
        });
        builder.add(RARE_POOL_KEY, boot -> {
            boot.register(EXAMPLE_POOL2, new RarePool("fuck_pool", List.of(Items.STONE.getDefaultInstance()), List.of(), List.of(Items.DIAMOND.getDefaultInstance())));
        });

        generator.addProvider(
                evt.includeServer(),
                new DatapackBuiltinEntriesProvider(
                        generator.getPackOutput(),
                        evt.getLookupProvider(),
                        builder,
                        Set.of(Solaris_compat.MODID)
                )
        );
    }
}
