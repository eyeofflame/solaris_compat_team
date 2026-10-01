package dev.efm.solaris_core.functions;

import dev.efm.solaris_core.config.SolarisConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 自动定时清理怪物管理器
 * <p>
 * ★ 完全不用 tick 计数 — 全部通过 Java ScheduledExecutorService 调度
 * ★ 真实时间驱动：不受服务器卡顿影响，到点就执行
 * <p>
 * 流程：
 * ① 定时器等待 (interval - 10) 秒
 * ② 广播 10 秒倒计时提醒（逐秒倒数）
 * ③ 10 秒后执行清理
 * ④ 回到 ①，循环
 */
@Mod.EventBusSubscriber
public class MobCleanupManager {

    // ==================== 参数配置 ====================

    /**
     * 清理间隔（秒），可改
     */
    private static long CLEANUP_INTERVAL_SEC;   // 5 分钟
    /**
     * 倒计时秒数
     */
    private static final int COUNTDOWN_SEC = 5;
    /**
     * 是否逐秒倒数（true）还是只发一次提醒（false）
     */
    private static final boolean PER_SECOND_COUNTDOWN = true;

    // ==================== 调度器 ====================

    /**
     * 守护线程单线程调度器 — 定时执行，不依赖 tick
     */
    private static final ScheduledExecutorService SCHEDULER =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "MobCleanup");
                t.setDaemon(true);   // 服务器关闭时自动终止
                return t;
            });

    private static MinecraftServer server;
    private static ScheduledFuture<?> warningFuture;
    private static ScheduledFuture<?> cleanupFuture;

    // ==================== 生命周期 ====================

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        CLEANUP_INTERVAL_SEC = SolarisConfig.CLEAN_TIME.get();
        server = event.getServer();
        startCycle();
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        stopCycle();
    }

    // ==================== 调度逻辑 ====================

    /**
     * 启动新一轮循环
     */
    private static void startCycle() {
        if (server == null || !server.isRunning()) return;

        long warningDelay = Math.max(1, CLEANUP_INTERVAL_SEC - COUNTDOWN_SEC);

        // ① 等 (总间隔 - 10) 秒 → 发倒计时提醒
        warningFuture = SCHEDULER.schedule(() -> {
            // ★ 关键：定时器线程不能直接操作 Minecraft，必须投递到 server 主线程
            server.execute(MobCleanupManager::startCountdown);
        }, warningDelay, TimeUnit.SECONDS);
    }

    /**
     * 倒计时阶段
     */
    private static void startCountdown() {
        if (!server.isRunning()) return;

        if (PER_SECOND_COUNTDOWN) {
            // 逐秒倒数：10 → 9 → 8 → ... → 1
            scheduleSecondCountdown(COUNTDOWN_SEC);
        } else {
            // 只发一次提醒
            broadcast(Component.literal("⚠ 怪物清理将在 " + COUNTDOWN_SEC + " 秒后开始！")
                    .withStyle(ChatFormatting.RED));
            scheduleCleanup(COUNTDOWN_SEC);
        }
    }

    /**
     * 逐秒倒数的递归调度（非 tick！）
     * 每一秒调度下一个任务，实现 "10...9...8..." 的效果
     */
    private static void scheduleSecondCountdown(int remaining) {
        if (remaining <= 0) {
            doCleanup();
            startCycle();   // 循环
            return;
        }

        // 广播当前秒数
        broadcast(Component.literal("⏳ 怪物清理倒计时: " + remaining + " 秒")
                .withStyle(ChatFormatting.RED));

        // 1 秒后再次调用自己（remaining-1）
        SCHEDULER.schedule(() -> {
            server.execute(() -> scheduleSecondCountdown(remaining - 1));
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * 调度清理任务（非倒数模式用）
     */
    private static void scheduleCleanup(long delaySeconds) {
        cleanupFuture = SCHEDULER.schedule(() -> {
            server.execute(() -> {
                doCleanup();
                startCycle();   // 循环
            });
        }, delaySeconds, TimeUnit.SECONDS);
    }

    // ==================== 核心清理逻辑 ====================

    private static void doCleanup() {
        if (server == null || !server.isRunning()) return;

        int totalKilled = 0;

        for (ServerLevel level : server.getAllLevels()) {
            Iterator<Entity> it = level.getEntities().getAll().iterator();
            while (it.hasNext()) {
                Entity entity = it.next();
                if (!(entity instanceof Monster monster)) continue;
                // 保留"有名字"的和"永久存在"的怪物（如命名过的僵尸、女巫小屋的女巫等）
                if (monster.isPersistenceRequired()) continue;
                if (monster.hasCustomName()) continue;
                // 保留骑乘和被骑乘的
                if (monster.isPassenger() || !monster.getPassengers().isEmpty()) continue;
                // 保留 Leash 拴着的
                if (monster.isLeashed()) continue;

                monster.remove(Entity.RemovalReason.DISCARDED);
                totalKilled++;
            }
        }

        // 广播结果
        Component msg;
        if (totalKilled > 0) {
            msg = Component.literal("✔ 怪物清理完成！共清除 " + totalKilled + " 只怪物")
                    .withStyle(ChatFormatting.GREEN);
        } else {
            msg = Component.literal("✔ 怪物清理完成！范围内无怪物需要清除")
                    .withStyle(ChatFormatting.GRAY);
        }
        broadcast(msg);
    }

    // ==================== 工具方法 ====================

    private static void broadcast(Component msg) {
        if (server != null && server.isRunning()) {
            server.getPlayerList().broadcastSystemMessage(msg, false);
        }
    }

    private static void stopCycle() {
        if (warningFuture != null) warningFuture.cancel(false);
        if (cleanupFuture != null) cleanupFuture.cancel(false);
    }

    // ==================== 外部 API：手动触发 ====================

    /**
     * 供外部调用，立即执行一次清理（不打断现有周期）
     */
    public static void forceCleanupNow() {
        if (server == null || !server.isRunning()) return;
        server.execute(() -> {
            broadcast(Component.literal("⚡ 管理员触发了强制清理！")
                    .withStyle(ChatFormatting.YELLOW));
            doCleanup();
        });
    }
}
