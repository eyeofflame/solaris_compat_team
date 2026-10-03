package dev.efm.solaris_progress.mixins;

import dev.efm.solaris_progress.SolaConfig;
import dev.efm.solaris_progress.SettlementProtection;
import dev.ftb.mods.ftbchunks.api.Protection;
import dev.ftb.mods.ftbchunks.data.ClaimedChunkManagerImpl;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 聚落领地内允许与配置白名单中的实体交互（交易/骑乘/喂食/拴绳等）。
 *
 * <p>FTB Chunks 的全部实体交互路径（EntityInteract / EntityInteractSpecific /
 * 盔甲架 interactAt mixin）都汇聚到
 * {@code ClaimedChunkManagerImpl#shouldPreventInteraction(..., Protection.INTERACT_ENTITY, ...)}，
 * 这里对"聚落领地 + 白名单实体 + 玩家"在入口直接放行（返回 false = 不阻止），
 * 其余情况原样走 FTB 逻辑（团队属性 ENTITY_INTERACT_MODE=PRIVATE 拦截非成员）。</p>
 *
 * <p>目标类属于其它 mod（不参与混淆），必须 {@code remap = false}。</p>
 */
@Mixin(value = ClaimedChunkManagerImpl.class, remap = false)
public abstract class ClaimedChunkManagerMixin {
    @Inject(method = "shouldPreventInteraction", at = @At("HEAD"), cancellable = true)
    private void solaris$allowConfiguredEntityInteraction(Entity actor, InteractionHand hand, BlockPos pos,
                                                          Protection protection, Entity targetEntity,
                                                          CallbackInfoReturnable<Boolean> cir) {
        if (protection != Protection.INTERACT_ENTITY || targetEntity == null) return;
        if (!(actor instanceof Player player)) return;
        if (!SolaConfig.protectSettlement || !SettlementProtection.isAllowedInteraction(targetEntity)) return;
        if (SettlementProtection.isInSettlementClaim(player.level(), pos)) {
            cir.setReturnValue(false);
        }
    }
}
