package dev.efm.mekanism_agriculture.common.lang;

import dev.efm.mekanism_agriculture.MekanismAgriculture;
import mekanism.api.text.ILangEntry;
import net.minecraft.Util;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public enum MekLang implements ILangEntry {

    MEKANISM_AGRICULTURE("constants", "mod_name"),
    PACK_DESCRIPTION("constants", "pack_description"),
    DESC_MEK_INFUSIONER("description","mek_infusioner");

    MekLang(String type, String path) {
        this(Util.makeDescriptionId(type, ResourceLocation.fromNamespaceAndPath(MekanismAgriculture.MODID, path)));
    }

    MekLang(String key) {
        this.key = key;
    }

    private final String key;

    @Override
    public @NotNull String getTranslationKey() {
        return key;
    }
}
