package dev.efm.solaris_core.api.multiblock;

import dev.efm.solaris_core.SolarisCore;
import dev.efm.solaris_core.config.SolarisCoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 多方块的事件入口（只用 Forge 事件）：
 * <ul>
 *     <li>右键 trigger 方块：成型</li>
 *     <li><b>Shift + 触发物品 + 右键控制器（master）</b>：整座还原（包括控制器那一格）</li>
 * </ul>
 * 另外，直接挖部件方块也会走 {@code onRemove} 拆解，但被敲的那一格会留空。
 */
@Mod.EventBusSubscriber(modid = SolarisCore.MODID)
public final class MultiblockEvents {

    private MultiblockEvents() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        Player player = event.getEntity();
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack stack = event.getItemStack();
        BlockState clicked = level.getBlockState(pos);

        // ---- Shift + 触发物品 + 右键控制器：整座复原 ----
        if (player.isShiftKeyDown()) {
            if (!SolarisCoreConfig.isTriggerItem(stack)
                    || !(clicked.getBlock() instanceof SolarisPartBlock)
                    || !(level.getBlockEntity(pos) instanceof SolarisMasterBlockEntity master)) {
                return;
            }
            if (level.isClientSide) {
                event.setCanceled(true);
                return;
            }
            if (level instanceof ServerLevel serverLevel) {
                // brokenPos = null：包括控制器那一格在内，全部还原
                MultiblockHandler.disassemble(serverLevel, master, null);
                event.setCanceled(true);
            }
            return;
        }

        // ---- 普通右键：成型 ----
        if (!SolarisCoreConfig.isTriggerItem(stack)) {
            return;
        }
        if (!isAnyTrigger(clicked)) {
            return;
        }
        if (level.isClientSide) {
            // 客户端先取消本地预测，避免别的 mod 的 GUI 闪一下再被服务端顶掉
            event.setCanceled(true);
            return;
        }
        if (level instanceof ServerLevel && player instanceof ServerPlayer serverPlayer) {
            if (MultiblockHandler.tryForm(serverPlayer, pos, event.getFace())) {
                event.setCanceled(true);
            }
        }
    }

    public static boolean isAnyTrigger(BlockState state) {
        for (Multiblock multiblock : MultiblockRegistry.all()) {
            if (multiblock.isTrigger(state)) {
                return true;
            }
        }
        return false;
    }
}
