package dev.efm.solaris_compat.common.items;

import cofh.core.common.item.IAugmentItem;
import cofh.core.util.helpers.AugmentDataHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.List;

/**
 * 有机灌注器专属强化：提升运行速度，并有概率增产。
 * <p>
 * 速度走的是 Thermal 自己的强化数据通道——{@code MachinePower} 是加算属性，最终会乘进
 * {@code MachineBlockEntity.finalizeAttributes} 里的 {@code processTick}，所以 GUI 的
 * 速度条、RF 效率、能量消耗全都是原生表现。
 * <p>
 * 增产则没有原生通道可用：Thermal 的 {@code MachineSec}（辅助处理筛用的那个）只是给
 * <b>配方里显式写了概率的副产物</b>乘个系数，对 {@code chance < 0}（必出，灌注器产物就是这种）
 * 直接跳过不生效。所以增产是 {@code MachineBlockEntityMixin} 在
 * {@code resolveOutputs} 结束后自己算的。
 * <p>
 * 「只能装在灌注器上 + 同组互斥」由 {@code mixin.thermal} 下的 mixin 在强化槽校验时拦截。
 */
public class InsolatorSpeedUpgradeItem extends Item implements IAugmentItem {

    /**
     * {@code MachinePower} 是加算属性，原版机器速度升级给的是 1.0（即 +100%）。
     * 注意 {@code processStart} 会把 processTick 截断到配方能耗，
     * 所以再怎么堆也不会超过「1 tick 一次」。
     */
    private final float SPEED_MOD;

    private final int SPEED_PERCENT;

    /** 触发增产的概率，0~1。 */
    private final float BONUS_CHANCE;

    /**
     * 触发时主产物的产出倍数，>= 1（1 等于不增产）。可以是小数，比如 1.5。
     * <p>
     * 小数份额由 {@code InsolatorUpgradeHelper} 随机取整：整数部分必给，
     * 小数部分按概率给 1 个，长期平均正好是这么多倍。
     */
    private final float BONUS_MULTIPLIER;

    private final CompoundTag augmentData;

    /**
     * @param speedMod        加算进 MachinePower 的速度系数，0.5 表示 +50%
     * @param bonusChance     每次加工触发增产的概率，0~1
     * @param bonusMultiplier 触发时主产物的产出倍数，>= 1，可小数
     */
    public InsolatorSpeedUpgradeItem(float speedMod, float bonusChance, float bonusMultiplier) {
        super(new Properties());
        this.SPEED_MOD = Math.max(0.0F, speedMod);
        this.SPEED_PERCENT = Math.round(this.SPEED_MOD * 100.0F);
        this.BONUS_CHANCE = Math.min(1.0F, Math.max(0.0F, bonusChance));
        this.BONUS_MULTIPLIER = Math.max(1.0F, bonusMultiplier);
        this.augmentData = AugmentDataHelper.builder()
                .type("Machine")
                .mod("MachinePower", this.SPEED_MOD)
                .build();
    }

    public float getBonusChance() {
        return BONUS_CHANCE;
    }

    public float getBonusMultiplier() {
        return BONUS_MULTIPLIER;
    }

    @Override
    public @Nullable CompoundTag getAugmentData(ItemStack augment) {
        return augmentData;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.solaris.insolator.speed", SPEED_PERCENT).withStyle(ChatFormatting.AQUA));
        if (BONUS_MULTIPLIER > 1.0F && BONUS_CHANCE > 0.0F) {
            tooltip.add(Component.translatable("tooltip.solaris.insolator.bonus",
                    Math.round(BONUS_CHANCE * 100.0F),
                    // 只显示有效位：2.0 显示 "2"，1.5 显示 "1.5"
                    new DecimalFormat("0.##").format(BONUS_MULTIPLIER)).withStyle(ChatFormatting.AQUA));
        }
        tooltip.add(Component.translatable("tooltip.solaris.insolator.desc").withStyle(ChatFormatting.GRAY));
    }
}
