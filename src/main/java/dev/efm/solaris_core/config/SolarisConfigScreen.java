package dev.efm.solaris_core.config;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Cloth Config 配置界面。
 * <p>
 * 只有客户端、且安装了 cloth_config 时才会被加载（见 {@code SolarisCore#registerConfigScreen}）。
 * 目前只有一项：多方块触发物品列表（ResourceLocation 字符串）。
 */
public final class SolarisConfigScreen {

    private SolarisConfigScreen() {
    }

    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("config.solaris_core.title"))
                .setSavingRunnable(SolarisCoreConfig.SPEC::save);

        ConfigCategory multiblock = builder.getOrCreateCategory(Component.translatable("config.solaris_core.multiblock"));
        ConfigEntryBuilder entries = builder.entryBuilder();

        multiblock.addEntry(entries.startStrList(
                        Component.translatable("config.solaris_core.trigger_items"),
                        new ArrayList<>(SolarisCoreConfig.TRIGGER_ITEMS.get()))
                .setDefaultValue(List.of())
                // 必须展开：折叠状态下点 "+" 虽然会加一条，但看不到新行
                .setExpanded(true)
                .setInsertButtonEnabled(true)
                .setAddButtonTooltip(Component.translatable("config.solaris_core.trigger_items.add"))
                .setTooltip(Component.translatable("config.solaris_core.trigger_items.tooltip"))
                .setSaveConsumer(list -> SolarisCoreConfig.TRIGGER_ITEMS.set(list))
                .build());

        return builder.build();
    }
}
