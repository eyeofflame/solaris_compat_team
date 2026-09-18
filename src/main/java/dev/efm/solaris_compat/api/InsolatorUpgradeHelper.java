package dev.efm.solaris_compat.api;

import cofh.core.util.helpers.ItemHelper;
import cofh.lib.common.inventory.ItemStorageCoFH;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.expansion.common.block.entity.machine.MachineInsolatorBlockEntity;
import cofh.thermal.lib.util.recipes.IMachineInventory;
import cofh.thermal.lib.util.recipes.internal.IMachineRecipe;
import dev.efm.solaris_compat.common.items.InsolatorSpeedUpgradeItem;
import dev.efm.solaris_compat.mixin.thermal.AugmentableBlockEntityInvoker;
import dev.efm.solaris_compat.mixin.thermal.Reconfigurable4WayBlockEntityInvoker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.function.Predicate;

/**
 * 灌注器专属升级的校验与增产逻辑。
 * <p>
 * 特意放在普通类里而不是 mixin 中：{@code mixin.thermal} 下的几个 mixin 整体标了
 * {@code remap = false}，方法体里不能出现 Minecraft 的混淆成员引用，所以凡是需要
 * 碰 {@link ItemStack} 的活都由这里代劳。
 */
public final class InsolatorUpgradeHelper {

    /** 有机灌注器的方块注册名。 */
    public static final ResourceLocation MACHINE_INSOLATOR = ResourceLocation.fromNamespaceAndPath("thermal", "machine_insolator");

    private InsolatorUpgradeHelper() {
    }

    // ---------------------------------------------------------------- 校验

    /** 是不是灌注器专属的那几个升级。 */
    public static boolean isInsolatorUpgrade(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof InsolatorSpeedUpgradeItem;
    }

    /**
     * 这个「可强化物品」是不是有机灌注器的方块物品。
     * <p>
     * 工匠台给机器<b>物品</b>装强化时走的就是这条判断，得堵上，
     * 否则可以把升级塞进别的机器物品里再摆出来。
     */
    public static boolean isInsolatorMachine(ItemStack augmentable) {
        if (augmentable.isEmpty() || !(augmentable.getItem() instanceof BlockItem blockItem)) {
            return false;
        }
        return MACHINE_INSOLATOR.equals(ForgeRegistries.BLOCKS.getKey(blockItem.getBlock()));
    }

    /** 这批强化里是不是已经有灌注器专属升级了。 */
    public static boolean hasInsolatorUpgrade(List<ItemStack> augments) {
        for (ItemStack augment : augments) {
            if (isInsolatorUpgrade(augment)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 机器的强化槽过滤器：只允许装进灌注器，且几个专属升级之间互斥。
     * <p>
     * Thermal 原本的类型校验（{@code MACHINE_VALIDATOR}）照常生效，这里只是叠加规则。
     *
     * @param machine 机器方块实体，用来实时读取已装的强化（走 {@link AugmentableBlockEntityInvoker}）
     */
    public static Predicate<ItemStack> restrictMachineSlots(Predicate<ItemStack> original, boolean isInsolator, Object machine) {
        return stack -> {
            if (!original.test(stack)) {
                return false;
            }
            if (!isInsolatorUpgrade(stack)) {
                return true;
            }
            // 是我们的升级：既要是灌注器，还不能和已装的同类冲突
            return isInsolator && !hasInsolatorUpgrade(readAugments(machine));
        };
    }

    /**
     * 工匠台那条路的判断（给机器<b>物品</b>装强化）。
     * 这里强化列表是现成的参数，不用去方块实体上读。
     */
    public static boolean shouldRejectOnItem(ItemStack candidate, ItemStack augmentable, List<ItemStack> existingAugments) {
        if (!isInsolatorUpgrade(candidate)) {
            return false;
        }
        return !isInsolatorMachine(augmentable) || hasInsolatorUpgrade(existingAugments);
    }

    // ---------------------------------------------------------------- 增产

    /**
     * 加工完成后的概率增产：掷一次骰子，中了就把主产物（配方第 0 个产物）多给
     * {@code (倍数 - 1)} 份。
     * <p>
     * 由 {@code MachineBlockEntityMixin} 挂在 {@code resolveOutputs} 的末尾，
     * 也就是 Thermal 自己把产物放完之后。只做「往里加」，不动原有逻辑。
     * <p>
     * 挂在 TAIL 而不是改写 Thermal 的放置过程，是因为那里没法控制堆叠上限：
     * {@code ItemStack.grow()} 不会截断到最大堆叠数，直接放大产出数量会产出超堆叠的物品。
     *
     * @param recipe  当前配方，可能为 null（理论上不会，保险起见判一下）
     * @param machine 机器方块实体
     */
    public static void applyBonusOutput(IMachineRecipe recipe, Object machine) {
        if (recipe == null || !(machine instanceof MachineInsolatorBlockEntity)) {
            return;
        }
        InsolatorSpeedUpgradeItem upgrade = findInstalledUpgrade(machine);
        if (upgrade == null) {
            return;
        }
        float chance = upgrade.getBonusChance();
        float multiplier = upgrade.getBonusMultiplier();
        if (chance <= 0.0F || multiplier <= 1.0F || MathHelper.RANDOM.nextFloat() >= chance) {
            return;
        }
        if (!(machine instanceof IMachineInventory inventory)) {
            return;
        }
        List<ItemStack> recipeOutputs = recipe.getOutputItems(inventory);
        if (recipeOutputs.isEmpty()) {
            return;
        }
        ItemStack primary = recipeOutputs.get(0);
        if (primary.isEmpty()) {
            return;
        }
        int extra = rollExtra(primary.getCount() * (multiplier - 1.0F));
        if (extra > 0) {
            insertIntoOutputs(machine, primary, extra);
        }
    }

    /**
     * 浮点倍数会算出小数份额（×1.5、主产物 1 个 → 多出 0.5 个），这里做随机取整：
     * 整数部分必给，小数部分按概率给 1 个。
     * <p>
     * 不能向下取整——那样 ×1.5 在单产物时永远是 0（floor(0.5)），等于没效果；
     * 也不能四舍五入——那样 ×1.5 会恒定变成 ×2，长期偏多。随机取整的期望值正好是原数。
     */
    private static int rollExtra(float amount) {
        int whole = (int) amount;
        return MathHelper.RANDOM.nextFloat() < amount - (float) whole ? whole + 1 : whole;
    }

    /**
     * 往产出槽里塞东西：先补到已有的同类堆上，再占空槽，都不会超过最大堆叠数。
     * 空间不够时能塞多少塞多少（多出来的就丢了，不会去撑爆槽位）。
     */
    private static void insertIntoOutputs(Object machine, ItemStack template, int amount) {
        List<ItemStorageCoFH> slots = readOutputSlots(machine);
        int remaining = amount;

        for (ItemStorageCoFH slot : slots) {
            if (remaining <= 0) {
                return;
            }
            ItemStack stack = slot.getItemStack();
            if (!stack.isEmpty() && ItemHelper.itemsEqualWithTags(stack, template)) {
                int room = stack.getMaxStackSize() - stack.getCount();
                if (room > 0) {
                    int moved = Math.min(room, remaining);
                    stack.grow(moved);
                    remaining -= moved;
                }
            }
        }

        for (ItemStorageCoFH slot : slots) {
            if (remaining <= 0) {
                return;
            }
            if (slot.isEmpty()) {
                int moved = Math.min(template.getMaxStackSize(), remaining);
                slot.setItemStack(ItemHelper.cloneStack(template, moved));
                remaining -= moved;
            }
        }
    }

    /** 找已装的那一个专属升级。互斥保证了最多只有一个。 */
    private static InsolatorSpeedUpgradeItem findInstalledUpgrade(Object machine) {
        for (ItemStack augment : readAugments(machine)) {
            if (augment.getItem() instanceof InsolatorSpeedUpgradeItem upgrade) {
                return upgrade;
            }
        }
        return null;
    }

    /** 每次现读，不要缓存返回值。 */
    private static List<ItemStack> readAugments(Object machine) {
        return machine instanceof AugmentableBlockEntityInvoker invoker
                ? invoker.solaris$getAugmentsAsList()
                : List.of();
    }

    private static List<ItemStorageCoFH> readOutputSlots(Object machine) {
        return machine instanceof Reconfigurable4WayBlockEntityInvoker invoker
                ? invoker.solaris$outputSlots()
                : List.of();
    }
}
