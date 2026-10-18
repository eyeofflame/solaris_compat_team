package dev.efm.solaris_resources.mixin;

import com.hollingsworth.arsnouveau.common.block.tile.RelaySplitterTile;
import dev.efm.solaris_resources.common.source.SourceTileProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 分流型中继器的传输循环同样写死了 {@code instanceof AbstractSourceMachine}，
 * 与 {@link RelayTileMixin} 一样在取值处套代理。
 */
@Mixin(RelaySplitterTile.class)
public abstract class RelaySplitterTileMixin {

    @Redirect(
            method = {"processFromList", "processToList"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;getBlockEntity(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/entity/BlockEntity;"),
            require = 1)
    private BlockEntity solaris$wrapSourceTile(Level level, BlockPos pos) {
        return SourceTileProxy.wrap(level.getBlockEntity(pos));
    }
}
