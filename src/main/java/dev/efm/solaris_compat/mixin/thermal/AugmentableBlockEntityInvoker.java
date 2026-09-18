package dev.efm.solaris_compat.mixin.thermal;

import cofh.thermal.lib.common.block.entity.AugmentableBlockEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

/**
 * {@code AugmentableBlockEntity.getAugmentsAsList()} 是 {@code protected}，我们包外调不到，
 * 用 {@code @Invoker} 开个口子。
 * <p>
 * 注意它每次调用都会重新扫一遍槽位并返回新列表，所以调用方必须<b>每次都问</b>，
 * 不能把返回的列表缓存起来当快照用（那样互斥判断会失效）。
 */
@Mixin(value = AugmentableBlockEntity.class, remap = false)
public interface AugmentableBlockEntityInvoker {

    @Invoker(value = "getAugmentsAsList", remap = false)
    List<ItemStack> solaris$getAugmentsAsList();
}
