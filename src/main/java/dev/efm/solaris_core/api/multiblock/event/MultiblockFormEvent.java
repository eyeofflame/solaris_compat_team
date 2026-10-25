package dev.efm.solaris_core.api.multiblock.event;

import dev.efm.solaris_core.api.multiblock.Multiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.eventbus.api.Event;

/**
 * 多方块即将成型时在 {@code MinecraftForge.EVENT_BUS} 上发布；取消即可阻止成型。
 * <p>
 * 只在服务端发布。适合做权限检查、配方/研究解锁、统计等。
 */
@Cancelable
public class MultiblockFormEvent extends Event {

    private final ServerLevel level;
    private final BlockPos controllerPos;
    private final Multiblock multiblock;
    private final Player player;

    public MultiblockFormEvent(ServerLevel level, BlockPos controllerPos, Multiblock multiblock, Player player) {
        this.level = level;
        this.controllerPos = controllerPos.immutable();
        this.multiblock = multiblock;
        this.player = player;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public BlockPos getControllerPos() {
        return controllerPos;
    }

    public Multiblock getMultiblock() {
        return multiblock;
    }

    public Player getPlayer() {
        return player;
    }
}
