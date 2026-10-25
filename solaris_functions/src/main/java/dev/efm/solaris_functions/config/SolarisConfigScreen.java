package dev.efm.solaris_functions.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class SolarisConfigScreen {
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("title.solaris_functions.config"));

        ConfigCategory category = builder.getOrCreateCategory(Component.translatable("category.solaris_functions.general"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        category.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.solaris_functions.enableAlwatsEat"), SolarisConfig.ENABLE_EAT.get())
                        .setTooltip(Component.translatable("tooltip.solaris_functions.enableAlwaysEat"))
                        .setSaveConsumer(SolarisConfig.ENABLE_EAT::set)
                        .setDefaultValue(true)
                        .build()
        );

        category.addEntry(
                entryBuilder.startBooleanToggle(Component.translatable("option.solaris_functions.enableResProc"), SolarisConfig.ENABLE_RESPROC.get())
                        .setTooltip(Component.translatable("tooltip.solaris_functions.enableResProc"))
                        .setSaveConsumer(SolarisConfig.ENABLE_RESPROC::set)
                        .setDefaultValue(true)
                        .build()
        );

        category.addEntry((
                entryBuilder.startIntField(Component.translatable("option.solaris_functions.resProcTime"),SolarisConfig.RESPROC_TIME.get())
                        .setTooltip(Component.translatable("tooltip.solaris_functions.resProcTime"))
                        .setSaveConsumer(SolarisConfig.RESPROC_TIME::set)
                        .setDefaultValue(10)
                        .build()
                ));

        category.addEntry(
                entryBuilder.startIntField(Component.translatable("option.solaris_functions.clean_time"),SolarisConfig.CLEAN_TIME.get())
                        .setDefaultValue(5*60)
                        .setSaveConsumer(SolarisConfig.CLEAN_TIME::set)
                        .setTooltip(Component.translatable("tip.solaris_functions.define_clean_time"))
                        .build()
        );

        return builder.build();
    }
}
