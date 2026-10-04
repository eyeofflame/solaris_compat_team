package dev.efm.solaris_compat.command;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.efm.solaris_compat.data.reader.TradeConfigLoader;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * {@code /sola_export}：把所有已注册的村民职业 id 导出到
 * {@code config/solaris_compat/trade/<namespace>/<path>.json}，文件内容只有
 * {@code {"profession":"<id>"}}。
 *
 * <p>命名空间作为一级子目录（如 {@code minecraft:farmer} →
 * {@code minecraft/farmer.json}）；path 自身带 {@code /} 时替换成 {@code _}，
 * 避免建出更深层目录。真实 id 以 JSON 内容为准。
 */
public final class SolaExportCommand {

    private static final Gson GSON = new Gson();

    private SolaExportCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("sola_export")
                        .requires(source -> source.hasPermission(2))
                        .executes(ctx -> export(ctx.getSource()))
        );
    }

    private static int export(CommandSourceStack source) {
        // 和 TradeConfigLoader 共用同一个目录，避免两边写岔
        Path dir = FMLPaths.CONFIGDIR.get().resolve(TradeConfigLoader.TRADE_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            source.sendFailure(Component.literal("创建导出目录失败: " + dir + " —— " + e.getMessage()));
            return 0;
        }

        int written = 0;
        int failed = 0;
        for (ResourceLocation id : ForgeRegistries.VILLAGER_PROFESSIONS.getKeys()) {
            JsonObject json = new JsonObject();
            json.addProperty("profession", id.toString());
            Path file = dir.resolve(id.getNamespace()).resolve(id.getPath().replace('/', '_') + ".json");
            try {
                Files.createDirectories(file.getParent());
                Files.writeString(file, GSON.toJson(json), StandardCharsets.UTF_8);
                written++;
            } catch (IOException e) {
                failed++;
            }
        }

        if (failed > 0) {
            source.sendFailure(Component.literal("导出村民职业: 成功 " + written + " 个，失败 " + failed + " 个（目录: " + dir + "）"));
            return 0;
        }
        int count = written;
        source.sendSuccess(() -> Component.literal("已导出 " + count + " 个村民职业到 " + dir), true);
        return count;
    }
}
