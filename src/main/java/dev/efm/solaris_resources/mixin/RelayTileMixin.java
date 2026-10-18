package dev.efm.solaris_resources.mixin;

import com.hollingsworth.arsnouveau.common.block.tile.RelayTile;
import dev.efm.solaris_resources.common.source.SourceTileProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 让魔源中继器把"只实现 ISourceTile 的机器"也当成合法的传输目标/来源。
 * <p>
 * 原实现把 {@code level.getBlockEntity(pos) instanceof AbstractSourceMachine} 写死在绑定与 tick 里，
 * 这里只替换取值处，把非 AbstractSourceMachine 的魔源容器包一层 {@link SourceTileProxy}，
 * 中继器自己的逻辑（距离、消息、粒子、红石开关）原样不动。
 */
@Mixin(RelayTile.class)
public abstract class RelayTileMixin {

    @Redirect(
            method = {"setSendTo", "onFinishedConnectionFirst", "onFinishedConnectionLast", "tick"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"),
            require = 1)
    private BlockEntity solaris$wrapSourceTile(Level level, BlockPos pos) {
        return SourceTileProxy.wrap(level.getBlockEntity(pos));
    }
}
