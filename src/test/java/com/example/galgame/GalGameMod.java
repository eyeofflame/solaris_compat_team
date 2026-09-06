package com.example.galgame;

import com.example.galgame.api.CharacterProfile;
import com.example.galgame.api.GalGameScript;
import com.example.galgame.client.GalGameClient;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;

/**
 * 示例 mod 入口：把 GalGame 对话系统接入 Minecraft 1.20.1 Forge。
 *
 * <p>默认按 <b>N</b> 键打开一段演示剧本。你可以把 {@link #characters()} 的角色表
 * 换成自己的立绘贴图，再用 {@link GalGameClient#open} 在你想要的时机（NPC 交互、
 * 任务剧情、按键、命令）触发演出。</p>
 */
@Mod(GalGameMod.MODID)
public final class GalGameMod {

    public static final String MODID = "galgame";

    /**
     * 打开对话的按键（默认 N）。
     */
    public static final KeyMapping OPEN_KEY = new KeyMapping(
            "key.galgame.open",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_N,
            "category.galgame");

    /**
     * 演示角色表（含两份立绘命名空间；实际贴图请按注释放置）。
     */
    private static final Map<String, CharacterProfile> CHARACTERS = new HashMap<>();

    static {
        CHARACTERS.put("alice", new CharacterProfile("galgame.char.alice")
                .pose("base", new ResourceLocation(MODID, "galgame/portraits/alice/base"))
                .pose("happy", new ResourceLocation(MODID, "galgame/portraits/alice/happy")));
        CHARACTERS.put("hero", new CharacterProfile("galgame.char.hero")
                .pose("base", new ResourceLocation(MODID, "galgame/portraits/hero/base")));
    }

    public GalGameMod() {
        DistExecutor.safeRunWhenOn(Dist.CLIENT, () -> GalGameMod::registerClientOnly);
    }

    /**
     * 仅客户端注册：按键映射 + ClientTick 事件。
     */
    private static void registerClientOnly() {
        FMLJavaModLoadingContext.get().getModEventBus()
                .addListener((RegisterKeyMappingsEvent e) -> e.register(OPEN_KEY));
        MinecraftForge.EVENT_BUS.addListener(GalGameMod::onClientTick);
    }

    /**
     * 按键触发打开演示剧本。
     */
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (OPEN_KEY.consumeClick()) {
            GalGameClient.open(demoScript(), CHARACTERS, null);
        }
    }

    /**
     * 返回角色注册表（可整体替换为你自己的角色）。
     */
    public static Map<String, CharacterProfile> characters() {
        return CHARACTERS;
    }

    /**
     * 一段演示剧本，展示背景/立绘/对白/旁白/选项/跳转/结束等全部能力。
     */
    public static GalGameScript demoScript() {
        return new GalGameScript.Builder()
                .label("start")
                .background(new ResourceLocation(MODID, "galgame/bg/room"))
                .show("alice", "happy")
                .say("alice", "你终于来了！我等你好久啦……", "happy")
                .say(null, "（窗外阳光正好，少女微微侧过头。）")
                .hidePortrait()
                .show("hero", "base")
                .say("hero", "路上耽搁了一会儿，抱歉。")
                .show("alice", "base")
                .choice(b -> b.opt("要一起去野餐吗？", "picnic")
                        .opt("今天想休息一下……", "rest"))
                .label("picnic")
                .say("alice", "好耶！那我们出发吧！", "happy")
                .jump("end")
                .label("rest")
                .say("alice", "唔……那好吧，改天一定要去哦！")
                .label("end")
                .say("alice", "今天真的很开心。", "happy")
                .end()
                .build();
    }
}
