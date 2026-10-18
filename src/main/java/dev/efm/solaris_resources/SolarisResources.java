package dev.efm.solaris_resources;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.lowdragmc.lowdraglib.gui.editor.data.UIProject;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;
import dev.efm.solaris_resources.common.config.AtomConfigs;
import dev.efm.solaris_resources.common.config.MachineConfigs;
import dev.efm.solaris_resources.common.registration.BlockRegistries;
import dev.efm.solaris_resources.common.registration.CreativeTabRegistries;
import dev.efm.solaris_resources.common.registration.EntityTypeRegistries;
import dev.efm.solaris_resources.common.registration.ItemRegistries;
import dev.efm.solaris_resources.common.spells.EffectAggregationAtom;
import dev.efm.solaris_resources.common.spells.EffectAssemblyAtom;
import dev.efm.solaris_resources.common.spells.EffectConvertWater;
import dev.efm.solaris_resources.common.spells.EffectDissolveWater;
import dev.efm.solaris_resources.common.worldgen.OreGenerationRules;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Mod(SolarisResources.MODID)
public class SolarisResources {

    public static final String MODID = "solaris_resources";

    public static final Random RND = new Random();

    private static final Map<ResourceLocation, Supplier<WidgetGroup>> UI_TEMPLATES = new ConcurrentHashMap<>();

    public SolarisResources(FMLJavaModLoadingContext context) {
        IEventBus ibus = context.getModEventBus(), fbus = MinecraftForge.EVENT_BUS;

        ItemRegistries.register(ibus);
        CreativeTabRegistries.register(ibus);
        EntityTypeRegistries.register(ibus);
        ibus.addListener(this::commonSetup);

        fbus.addListener(ItemRegistries::itemTip);
        BlockRegistries.register(ibus);

        registerSpell();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            AtomConfigs.STORE.reload();
            OreGenerationRules.reload();
            MachineConfigs.MAGIC_CORE.reload();
        });
    }

    private static void registerSpell() {
        GlyphRegistry.registerSpell(EffectConvertWater.INSTANCE);
        GlyphRegistry.registerSpell(EffectDissolveWater.INSTANCE);
        GlyphRegistry.registerSpell(EffectAggregationAtom.INSTANCE);
        GlyphRegistry.registerSpell(EffectAssemblyAtom.INSTANCE);
    }

    /**
     * 从 jar 内 {@code assets/<ns>/efm_ui/<name>.ui} 读取 UI 模板。
     * 缓存的是 {@link UIProject#loadUIFromTag} 返回的 Supplier：它缓存资源解析，
     * 但每次 {@code get()} 都建一棵新 widget 树（widget 有状态，不能跨次复用）。
     */
    public static Supplier<WidgetGroup> readUI(ResourceLocation id) {
        return UI_TEMPLATES.computeIfAbsent(id, key -> {
            String path = "/assets/" + key.getNamespace() + "/efm_ui/" + key.getPath() + ".ui";
            try (InputStream in = SolarisResources.class.getResourceAsStream(path)) {
                if (in == null) throw new IllegalStateException("UI template missing: " + path);
                CompoundTag tag = NbtIo.read(new DataInputStream(in), NbtAccounter.UNLIMITED);
                return UIProject.loadUIFromTag(tag);
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read " + path, e);
            }
        });
    }
}
