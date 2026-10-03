package dev.efm.solaris_progress;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.ftb.mods.ftbchunks.api.ClaimResult;
import dev.ftb.mods.ftbchunks.api.ClaimedChunk;
import dev.ftb.mods.ftbchunks.api.ClaimedChunkManager;
import dev.ftb.mods.ftbchunks.api.ChunkTeamData;
import dev.ftb.mods.ftbchunks.api.FTBChunksAPI;
import dev.ftb.mods.ftbchunks.api.FTBChunksProperties;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.TeamManager;
import dev.ftb.mods.ftbteams.api.property.PrivacyMode;
import dev.ftb.mods.ftbteams.api.property.TeamProperties;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 聚落领地：把 spawn_flatland 区域圈为服务器团队 {@link #TEAM_NAME} 的 FTB Chunks 领地。
 *
 * <p>启动时幂等执行：团队不存在则创建；已认领的区块跳过；他人领地绝不夺取（只记警告）。
 * 方块破坏/交互等由 FTB Chunks 原生保护负责（团队隐私属性设为 PRIVATE），
 * 生物受击由 {@link SettlementProtection} 补拦。</p>
 */
public final class SettlementClaims {
    public static final String TEAM_NAME = "efm_server";
    /** 领地在地图上的颜色（青蓝色）；FTB Chunks 用它以 alpha 100 半透明覆盖整个区域。 */
    public static final int TEAM_COLOR_RGB = 0x00BCD4;
    private static final String TEAM_DESCRIPTION = "Settlement protection area (auto-created)";
    private static final Logger LOGGER = LoggerFactory.getLogger("solaris_progress");

    private SettlementClaims() {}

    /** 服务端启动时调用，幂等。 */
    public static void ensure(MinecraftServer server) {
        if (!SolaConfig.protectSettlement) {
            LOGGER.info("settlement: disabled (protectSettlement=false)");
            return;
        }
        if (!FTBTeamsAPI.api().isManagerLoaded() || !FTBChunksAPI.api().isManagerLoaded()) {
            LOGGER.warn("settlement: FTB Teams/Chunks manager not loaded yet, skipping");
            return;
        }

        Team team = findTeamByName(FTBTeamsAPI.api().getManager(), TEAM_NAME);
        if (team != null && !team.isServerTeam()) {
            LOGGER.error("settlement: team '{}' exists but is not a server team, skipping", TEAM_NAME);
            return;
        }
        if (team == null) {
            team = createSettlementTeam(server);
            if (team == null) return;
            LOGGER.info("settlement: created server team '{}' ({})", TEAM_NAME, team.getId());
        } else {
            LOGGER.info("settlement: using existing server team '{}' ({})", TEAM_NAME, team.getId());
        }

        applyTeamProperties(team);
        claimRegion(server, team);
    }

    /** 团队是否为本模组的聚落保护团队（server team 且显示名匹配）。允许 null。 */
    public static boolean isSettlementTeam(Team team) {
        return team != null && team.isServerTeam()
                && TEAM_NAME.equals(team.getProperty(TeamProperties.DISPLAY_NAME));
    }

    /** 按显示名扫描全部团队。注意 {@code getTeamByName} 匹配的是 "名字#uuid前8位"，不能用。 */
    private static Team findTeamByName(TeamManager manager, String name) {
        for (Team team : manager.getTeams()) {
            if (name.equals(team.getProperty(TeamProperties.DISPLAY_NAME))) {
                return team;
            }
        }
        return null;
    }

    private static Team createSettlementTeam(MinecraftServer server) {
        try {
            return FTBTeamsAPI.api().getManager()
                    .createServerTeam(source(server), TEAM_NAME, TEAM_DESCRIPTION, null);
        } catch (CommandSyntaxException e) {
            LOGGER.error("settlement: failed to create team '{}': {}", TEAM_NAME, e.getMessage());
            return null;
        }
    }

    /** 每次启动都写，幂等。setProperty 标脏，随世界存档持久化。PvP 属性不动，沿用 FTB 规则。 */
    private static void applyTeamProperties(Team team) {
        team.setProperty(FTBChunksProperties.BLOCK_EDIT_MODE, PrivacyMode.PRIVATE);
        team.setProperty(FTBChunksProperties.BLOCK_INTERACT_MODE, PrivacyMode.PRIVATE);
        team.setProperty(FTBChunksProperties.ENTITY_INTERACT_MODE, PrivacyMode.PRIVATE);
        team.setProperty(FTBChunksProperties.NONLIVING_ENTITY_ATTACK_MODE, PrivacyMode.PRIVATE);
        team.setProperty(FTBChunksProperties.ALLOW_EXPLOSIONS, false);
        team.setProperty(FTBChunksProperties.ALLOW_MOB_GRIEFING, false);
        // 地图颜色：FTB Chunks 以 alpha 100 覆盖整个领地（描边由 RenderMapImageTaskMixin 去掉）
        team.setProperty(TeamProperties.COLOR, Color4I.rgb(TEAM_COLOR_RGB));
    }

    private static void claimRegion(MinecraftServer server, Team team) {
        ClaimedChunkManager manager = FTBChunksAPI.api().getManager();
        ChunkTeamData data = manager.getOrCreateData(team);
        CommandSourceStack source = source(server);

        int minChunkX = SettlementClaimMath.minChunk((int) SolaConfig.centerX, (int) SolaConfig.halfExtent);
        int maxChunkX = SettlementClaimMath.maxChunk((int) SolaConfig.centerX, (int) SolaConfig.halfExtent);
        int minChunkZ = SettlementClaimMath.minChunk((int) SolaConfig.centerZ, (int) SolaConfig.halfExtent);
        int maxChunkZ = SettlementClaimMath.maxChunk((int) SolaConfig.centerZ, (int) SolaConfig.halfExtent);

        int claimed = 0;
        int alreadyOwned = 0;
        int foreign = 0;
        int failed = 0;
        Map<String, Integer> foreignTeams = new LinkedHashMap<>();

        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                ChunkDimPos pos = new ChunkDimPos(Level.OVERWORLD, cx, cz);
                ClaimedChunk existing = manager.getChunk(pos);
                if (existing != null) {
                    if (existing.getTeamData().getTeam().getId().equals(team.getId())) {
                        alreadyOwned++;
                    } else {
                        foreign++;
                        foreignTeams.merge(existing.getTeamData().getTeam().getShortName(), 1, Integer::sum);
                    }
                    continue;
                }
                ClaimResult result = data.claim(source, pos, false);
                if (result != null && result.isSuccess()) {
                    claimed++;
                } else {
                    failed++;
                    LOGGER.warn("settlement: claim failed at [{}, {}]: {}",
                            cx, cz, result != null ? result.getResultId() : "null");
                }
            }
        }

        LOGGER.info("settlement: region chunks x{}..{} z{}..{} ({} chunks) -> claimed={}, alreadyOwned={}, foreignSkipped={}, failed={}",
                minChunkX, maxChunkX, minChunkZ, maxChunkZ,
                (maxChunkX - minChunkX + 1) * (maxChunkZ - minChunkZ + 1),
                claimed, alreadyOwned, foreign, failed);
        if (foreign > 0) {
            LOGGER.warn("settlement: chunks owned by other teams: {}", foreignTeams);
        }
    }

    private static CommandSourceStack source(MinecraftServer server) {
        return server.createCommandSourceStack().withSuppressedOutput().withPermission(4);
    }
}
