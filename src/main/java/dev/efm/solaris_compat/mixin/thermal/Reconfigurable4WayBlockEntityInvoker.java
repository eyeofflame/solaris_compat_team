package dev.efm.solaris_compat.mixin.thermal;

import cofh.lib.common.inventory.ItemStorageCoFH;
import cofh.thermal.lib.common.block.entity.Reconfigurable4WayBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * {@code outputSlots()} 是 {@code Reconfigurable4WayBlockEntity} 里的 {@code protected} 方法，
 * {@code IMachineInventory} 也没暴露它，增产要往产出槽里塞额外产物，只能开个口子。
 */
@Mixin(value = Reconfigurable4WayBlockEntity.class, remap = false)
public interface Reconfigurable4WayBlockEntityInvoker {

    @Invoker(value = "outputSlots", remap = false)
    List<ItemStorageCoFH> solaris$outputSlots();
}
