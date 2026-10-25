package dev.efm.solaris_core.api.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;

/**
 * 一台多方块机器的行为逻辑，按 {@link Multiblock#getId()} 注册。
 * <p>
 * 方块与 BlockEntity 是共享的（{@link SolarisPartBlock} / {@link SolarisMasterBlockEntity}），
 * 不同机器的差异全部放在这里：成型回调、tick、掉落、面板。
 * <p>
 * 注册方式：
 * <pre>{@code
 * MultiblockLogics.register(COKE_OVEN.getId(), new IMultiblockLogic() {
 *     @Override public void onBroken(ServerLevel level, SolarisMasterBlockEntity master) { ... }
 * });
 * }</pre>
 */
public interface IMultiblockLogic {

    /** 成型成功后调用（服务端）。 */
    default void onFormed(ServerLevel level, SolarisMasterBlockEntity master) {
    }

    /**
     * 结构被破坏、即将还原成材料之前调用（服务端）。
     * 机器还在，适合在这里掉库存、停声音、清数据。
     */
    default void onBroken(ServerLevel level, SolarisMasterBlockEntity master) {
    }

    /** 每 tick 调用（只有 master 有 ticker）。 */
    default void tick(ServerLevel level, SolarisMasterBlockEntity master) {
    }

    /**
     * 玩家右键机器任意一格时调用（服务端）。
     * <p>
     * 打开面板、播声音、发消息都由这里自己处理，框架不再自动开界面：
     * <pre>{@code
     * @Override
     * public void onInteract(ServerPlayer player, SolarisMasterBlockEntity master) {
     *     NetworkHooks.openScreen(player, new SimpleMenuProvider(...),
     *             buffer -> buffer.writeBlockPos(master.getBlockPos()));
     * }
     * }</pre>
     */
    default void onInteract(ServerPlayer player, SolarisMasterBlockEntity master) {
    }

    /**
     * 按"结构内相对位置 + 面"提供能力（可选）。
     * <p>
     * dummy 方块被别的 mod 查询能力时会转到 master，master 再调用这里；
     * 返回 {@link LazyOptional#empty()} 表示不接管，会退回 master 自己的（全局）能力。
     */
    default <T> LazyOptional<T> getCapabilityAt(SolarisMasterBlockEntity master, BlockPos posInMB,
                                                Capability<T> cap, @Nullable Direction side) {
        return LazyOptional.empty();
    }

    /**
     * 注册 GeckoLib 动画控制器（可选）。机器有 .animation.json 时在这里加 {@code AnimationController}。
     */
    default void registerControllers(SolarisMasterBlockEntity master, AnimatableManager.ControllerRegistrar controllers) {
    }
}
