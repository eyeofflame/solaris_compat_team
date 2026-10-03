package dev.efm.solaris_progress;

import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 聚落保护的服务端事件入口：启动时确保领地，运行期补 FTB Chunks 的保护缺口。
 *
 * <p>FTB Chunks 原生已拦：破坏/放置方块、右键交互方块、流体、右键实体、攻击非生物实体。
 * 本类补上两类缺口：① 攻击<b>生物</b>（FTB 只处理非生物）；② 领地内<b>使用物品</b>
 * （FTB 的 RIGHT_CLICK_ITEM 只拦少量黑名单物品，吃/喝/弓箭/投掷等默认放行）。</p>
 */
public final class SettlementProtection {
    private SettlementProtection() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        SettlementClaims.ensure(event.getServer());
    }

    /**
     * 领地内的无害生物（村民/动物/Olivia 等）不能被玩家（含投掷物）伤害。
     *
     * <p>玩家受害者交给 FTB Chunks 原有 PvP 规则；敌对怪物（{@link Enemy}）允许攻击；
     * 旁观模式、/ftbchunks admin bypass_protection 与团队成员放行。</p>
     */
    @SubscribeEvent
    public static void onLivingAttack(LivingAttackEvent event) {
        if (!SolaConfig.protectSettlement) return;

        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide) return;
        if (victim instanceof Player) return;
        if (victim instanceof Enemy) return;

        Entity attacker = event.getSource().getEntity();
        if (attacker == null) attacker = event.getSource().getDirectEntity();
        if (!(attacker instanceof Player)) return;

        if (shouldProtect(attacker, victim.level(), victim.blockPosition())) {
            event.setCanceled(true);
        }
    }

    /**
     * 领地内禁止使用物品：吃/喝、弓与弩、投掷物（末影珍珠/雪球/药水）、钓鱼竿、烟花等。
     *
     * <p>服务端权威路径 {@code ServerPlayerGameMode#useItem} 触发本事件，取消后物品的
     * {@code use()} 不会执行；对怪物的攻击性投掷已由 {@link #onLivingAttack} 兜底。</p>
     */
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (!SolaConfig.protectSettlement) return;

        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        if (shouldProtect(player, player.level(), player.blockPosition())) {
            event.setCanceled(true);
        }
    }

    /**
     * 领地内禁止用物品作用于方块（锄/锹/打火石/骨粉/桶/刷子等）。放置方块 FTB 原生已拦，
     * 这里按"目标方块所在区块"判定，与 FTB 的方块交互语义一致。
     */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!SolaConfig.protectSettlement) return;

        Player player = event.getEntity();
        if (player.level().isClientSide) return;

        if (shouldProtect(player, player.level(), event.getPos())) {
            event.setCanceled(true);
        }
    }

    /** 目标实体类型是否在配置的交互白名单中（{@code settlement.allowedInteractionEntities}）。 */
    public static boolean isAllowedInteraction(Entity target) {
        if (target == null) return false;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(target.getType());
        return id != null && SolaConfig.allowedInteractionEntities.contains(id.toString());
    }

    /** pos 是否位于聚落领地内（服务端；FTB Chunks 管理器未就绪时返回 false）。 */
    public static boolean isInSettlementClaim(Level level, BlockPos pos) {
        if (!FTBChunksAPI.api().isManagerLoaded()) return false;
        ClaimedChunk claim = FTBChunksAPI.api().getManager().getChunk(new ChunkDimPos(level, pos));
        return claim != null && SettlementClaims.isSettlementTeam(claim.getTeamData().getTeam());
    }

    /**
     * 服务端判定：actor 在 pos 处是否应被聚落保护阻止。
     * 旁观模式、/ftbchunks admin bypass_protection 与团队成员放行。
     */
    private static boolean shouldProtect(Entity actor, Level level, BlockPos pos) {
        if (!FTBChunksAPI.api().isManagerLoaded()) return false;

        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        ClaimedChunk claim = manager.getChunk(new ChunkDimPos(level, pos));
        if (claim == null || !SettlementClaims.isSettlementTeam(claim.getTeamData().getTeam())) return false;

        if (claim.getTeamData().isTeamMember(actor.getUUID())) return false;
        if (actor instanceof ServerPlayer serverPlayer
                && (serverPlayer.isSpectator() || manager.getBypassProtection(serverPlayer.getUUID()))) {
            return false;
        }
        return true;
    }
}
