package dev.efm.solaris_progress.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.efm.solaris_progress.SettlementClaims;
import dev.ftb.mods.ftbchunks.client.map.MapChunk;
import dev.ftb.mods.ftbchunks.client.map.RenderMapImageTask;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * 聚落领地在 FTB Chunks 地图（大地图与小地图共用烘焙贴图）上只保留半透明颜色覆盖，
 * 不画边缘的不透明描边（"框"）。
 *
 * <p>FTB Chunks 烘焙地图贴图时用 {@code !connects(相邻块)} 决定是否给领地边缘画实线；
 * 这里对聚落团队（{@link SettlementClaims#TEAM_NAME}）的区块把该判定恒置为 true，
 * 使 claimBar 全为 false。填充色来自团队 COLOR 属性（{@link SettlementClaims} 设为青蓝色，
 * FTB 以 alpha 100 叠加），此 mixin 不改变填充。</p>
 *
 * <p>目标类属于其它 mod（非 MC 类，不参与混淆），必须 {@code remap = false}，
 * 否则 mixin 注解处理器找不到混淆映射会编译失败，运行时也会错误重映射。</p>
 */
@Mixin(value = RenderMapImageTask.class, remap = false)
public abstract class RenderMapImageTaskMixin {
    @WrapOperation(
            method = "runMapTask",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/ftb/mods/ftbchunks/client/map/MapChunk;connects(Ldev/ftb/mods/ftbchunks/client/map/MapChunk;)Z"))
    private boolean solaris$hideSettlementClaimBorder(MapChunk chunk, MapChunk other, Operation<Boolean> original) {
        if (chunk.getTeam().map(SettlementClaims::isSettlementTeam).orElse(false)) {
            return true;
        }
        return original.call(chunk, other);
    }
}
