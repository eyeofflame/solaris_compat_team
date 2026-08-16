package dev.efm.solaris_compat.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ConfigScreen {
    public static Screen create(Screen parent){
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.solaris_compat.config"));

        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("category.solaris_compat.general"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        category.addEntry(
                entryBuilder.startIntField(Component.translatable("option.solaris_compat.flushTime"),SolarisConfig.FlushTime.get())
                        .setSaveConsumer(SolarisConfig.FlushTime::set)
                        .setDefaultValue(900)
                        .build()
        );

        return builder.build();
    }
}
