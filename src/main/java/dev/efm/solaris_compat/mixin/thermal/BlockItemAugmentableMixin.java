package dev.efm.solaris_compat.mixin.thermal;

import cofh.thermal.lib.common.item.BlockItemAugmentable;
import dev.efm.solaris_compat.api.InsolatorUpgradeHelper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * 堵住工匠台那条路：给机器<b>物品</b>装强化时走的是
 * {@code BlockItemAugmentable.validAugment}（即注册时传进去的
 * {@code ThermalAugmentRules.MACHINE_VALIDATOR}），跟机器运行时的强化槽是两套校验。
 * <p>
 * 不拦的话可以把灌注器升级塞进别的机器物品里，摆出来照样生效——因为
 * {@code AugmentableBlockEntity} 从 NBT 读回强化时不会再校验一遍。
 * <p>
 * 同样整体 {@code remap = false}，方法体里不碰 ItemStack 成员。
 */
@Mixin(value = BlockItemAugmentable.class, remap = false)
public class BlockItemAugmentableMixin {

    @Inject(
            method = "validAugment(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Ljava/util/List;)Z",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void solaris$restrictInsolatorUpgrade(ItemStack augmentable, ItemStack augment, List<ItemStack> augments, CallbackInfoReturnable<Boolean> cir) {
        if (InsolatorUpgradeHelper.shouldRejectOnItem(augment, augmentable, augments)) {
            cir.setReturnValue(false);
        }
    }
}
