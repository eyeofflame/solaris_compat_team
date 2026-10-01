package dev.efm.solaris_core.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SolarisConfigScreen {
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.solaris_core.config"));

        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("category.solaris_core.general"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        category.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.solaris_core.enableAlwatsEat"), SolarisConfig.ENABLE_EAT.get())
                        .setTooltip(Component.translatable("tooltip.solaris_core.enableAlwaysEat"))
                        .setSaveConsumer(SolarisConfig.ENABLE_EAT::set)
                        .setDefaultValue(true)
                        .build()
        );

        category.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.solaris_core.enableResProc"), SolarisConfig.ENABLE_RESPROC.get())
                        .setTooltip(Component.translatable("tooltip.solaris_core.enableResProc"))
                        .setSaveConsumer(SolarisConfig.ENABLE_RESPROC::set)
                        .setDefaultValue(true)
                        .build()
        );

        category.addEntry((
                entryBuilder.startIntField(Component.translatable("option.solaris_core.resProcTime"),SolarisConfig.RESPROC_TIME.get())
                        .setTooltip(Component.translatable("tooltip.solaris_core.resProcTime"))
                        .setSaveConsumer(SolarisConfig.RESPROC_TIME::set)
                        .setDefaultValue(10)
                        .build()
                ));

        category.addEntry(
                entryBuilder.startIntField(Component.translatable("option.solaris_core.clean_time"),SolarisConfig.CLEAN_TIME.get())
                        .setDefaultValue(5*60)
                        .setSaveConsumer(SolarisConfig.CLEAN_TIME::set)
                        .setTooltip(Component.translatable("tip.solaris_core.define_clean_time"))
                        .build()
        );

        return builder.build();
    }
}
