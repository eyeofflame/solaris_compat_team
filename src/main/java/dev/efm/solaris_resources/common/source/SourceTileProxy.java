package dev.efm.solaris_resources.common.source;

import com.hollingsworth.arsnouveau.api.source.AbstractSourceMachine;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 把只实现了 {@link ISourceTile} 的方块实体包装成 Ars Nouveau 的 {@link AbstractSourceMachine}。
 * <p>
 * 魔源中继器（{@code RelayTile} / {@code RelaySplitterTile}）在绑定和传输时硬编码了
 * {@code instanceof AbstractSourceMachine}，只实现 ISourceTile 的机器会被当成"不是魔源容器"，
 * 支配之杖因此绑定不上。这个代理只做类型适配：所有读写都转发给原方块实体，自己不保存任何状态。
 * <p>
 * 由 {@code RelayTileMixin} / {@code RelaySplitterTileMixin} 在 {@code Level#getBlockEntity} 的返回值上套用。
 */
public final class SourceTileProxy extends AbstractSourceMachine {

    private final ISourceTile delegate;

    private SourceTileProxy(BlockEntity owner, ISourceTile delegate) {
        super(owner.getType(), owner.getBlockPos(), owner.getBlockState());
        this.delegate = delegate;
    }

    /**
     * 目标是非 {@link AbstractSourceMachine} 的 {@link ISourceTile} 时返回转发代理，否则原样返回。
     */
    public static BlockEntity wrap(BlockEntity target) {
        if (target instanceof ISourceTile source && !(target instanceof AbstractSourceMachine)) {
            return new SourceTileProxy(target, source);
        }
        return target;
    }

    @Override
    public int getTransferRate() {
        return delegate.getTransferRate();
    }

    @Override
    public boolean canAcceptSource() {
        return delegate.canAcceptSource();
    }

    @Override
    public int getSource() {
        return delegate.getSource();
    }

    @Override
    public int getMaxSource() {
        return delegate.getMaxSource();
    }

    @Override
    public void setMaxSource(int max) {
        delegate.setMaxSource(max);
    }

    @Override
    public int setSource(int source) {
        return delegate.setSource(source);
    }

    @Override
    public int addSource(int source) {
        return delegate.addSource(source);
    }

    @Override
    public int removeSource(int source) {
        return delegate.removeSource(source);
    }
}
