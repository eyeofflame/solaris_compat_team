package dev.efm.solaris_compat.mixin.thermal;

import cofh.thermal.expansion.common.block.entity.machine.MachineInsolatorBlockEntity;
import cofh.thermal.lib.common.block.entity.MachineBlockEntity;
import cofh.thermal.lib.util.recipes.internal.IMachineRecipe;
import dev.efm.solaris_compat.api.InsolatorUpgradeHelper;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/**
 * 两件事：
 * <ol>
 *   <li>把机器的强化槽过滤器包一层——灌注器专属升级只能装进灌注器，
 *       且这几个升级之间互斥（一台只能装一个）。</li>
 *   <li>在加工结束时算概率增产。</li>
 * </ol>
 * <p>
 * 强化槽那边：{@code augValidator()} 在构造时就返回了槽位用的
 * {@code Predicate<ItemStack>}（{@code AugmentableBlockEntity.addAugmentSlots} 调用的），
 * 是唯一决定强化槽能放什么的入口，所以在那里包一层就够，没有别的机器子类重写它。
 * <p>
 * 注意整个 mixin 是 {@code remap = false}（目标是 CoFH 的类，本来就没混淆），
 * 因此方法体里不能直接碰 ItemStack 的成员，相关判断都丢给
 * {@link InsolatorUpgradeHelper}；把 {@code this} 原样传过去，
 * 让它自己去读实时的强化列表和产出槽。
 */
@Mixin(value = MachineBlockEntity.class, remap = false)
public class MachineBlockEntityMixin {

    @Shadow(remap = false)
    protected IMachineRecipe curRecipe;

    @Inject(
            method = "augValidator()Ljava/util/function/Predicate;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void solaris$restrictInsolatorUpgrade(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
        boolean isInsolator = (Object) this instanceof MachineInsolatorBlockEntity;
        cir.setReturnValue(InsolatorUpgradeHelper.restrictMachineSlots(cir.getReturnValue(), isInsolator, this));
    }

    /**
     * 挂在 {@code resolveOutputs} 末尾，也就是 Thermal 自己把产物放完之后，
     * 再按概率补一份主产物。挂在末尾而不是改写它的放置过程，是因为那样没法
     * 控制堆叠上限（详见 {@link InsolatorUpgradeHelper#applyBonusOutput}）。
     */
    @Inject(method = "resolveOutputs", at = @At("TAIL"), remap = false)
    private void solaris$applyBonusOutput(CallbackInfo ci) {
        InsolatorUpgradeHelper.applyBonusOutput(this.curRecipe, this);
    }
}
