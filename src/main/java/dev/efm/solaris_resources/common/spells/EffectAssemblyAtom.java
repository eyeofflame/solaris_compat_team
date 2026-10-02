package dev.efm.solaris_resources.common.spells;

import com.hollingsworth.arsnouveau.api.spell.*;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.entity.AtomItemEntity;
import dev.efm.solaris_resources.common.registration.ItemRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EffectAssemblyAtom extends AbstractEffect {
    public EffectAssemblyAtom() {
        super(
                ResourceLocation.fromNamespaceAndPath(SolarisResources.MODID, "assembly_atom"),
                "Assemble hydrogen atom to another atom."
        );
    }

    public static final EffectAssemblyAtom INSTANCE = new EffectAssemblyAtom();

    @Override
    protected int getDefaultManaCost() {
        return 50;
    }

    @Override
    protected @NotNull Set<AbstractAugment> getCompatibleAugments() {
        return Set.of();
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (world.isClientSide) return;

        BlockPos origin = rayTraceResult.getBlockPos();
        BlockPos min = origin.offset(-1, 1, -1);
        BlockPos max = origin.offset(1, 3, 1);

        List<AtomItemEntity> atoms = world.getEntitiesOfClass(AtomItemEntity.class, new AABB(min, max.offset(1, 1, 1)));

        for (AtomItemEntity atom : atoms) {
            if (!atom.getItem().getItem().equals(ItemRegistries.UNSTABLE_ATOM.get("hydrogen").get())) continue;

            int iron = 0, copper = 0, coal = 0;

            int count = atom.getItem().getCount();

            // 随机组装：每 12 份氢原子为一个最小单位。
            // 候选表必须在每轮循环内重建，否则旧的大比值会一直留在列表里被重复抽中，
            // 导致扣除的氢超过实际拥有量（凭空造资源）且 count 变成负数。
            while (count >= 12) {
                List<Integer> integers = new ArrayList<>();
                if (count >= 12) integers.add(12);
                if (count >= 56) integers.add(56);
                if (count >= 64) integers.add(64);

                int picked = integers.get(SolarisResources.RND.nextInt(integers.size()));

                switch (picked) {
                    case 12 -> {
                        coal++;
                        count -= 12;
                    }
                    case 56 -> {
                        iron++;
                        count -= 56;
                    }
                    case 64 -> {
                        copper++;
                        count -= 64;
                    }
                }

            }
            // 随机产量：产量在 [0, 理论最大数] 之间随机。
            // 用 nextInt(n + 1) 而不是 nextInt(n)：n 为 0 时也安全，且能取到最大值。
            int ironYield = SolarisResources.RND.nextInt(1, iron + 1);
            int copperYield = SolarisResources.RND.nextInt(1, copper + 1);
            int coalYield = SolarisResources.RND.nextInt(1, coal + 1);

            //spawn ItemEntity（产量为 0 时不生成空堆实体）
            if (ironYield > 0) {
                world.addFreshEntity(new AtomItemEntity(world, atom.getX() + SolarisResources.RND.nextDouble(-0.5, 0.5), atom.getY(), atom.getZ() + SolarisResources.RND.nextDouble(-0.5, 0.5), new ItemStack(ItemRegistries.UNSTABLE_ATOM.get("iron").get(), ironYield)));
            }
            if (copperYield > 0) {
                world.addFreshEntity(new AtomItemEntity(world, atom.getX() + SolarisResources.RND.nextDouble(-0.5, 0.5), atom.getY(), atom.getZ() + SolarisResources.RND.nextDouble(-0.5, 0.5), new ItemStack(ItemRegistries.UNSTABLE_ATOM.get("copper").get(), copperYield)));
            }
            if (coalYield > 0) {
                world.addFreshEntity(new AtomItemEntity(world, atom.getX() + SolarisResources.RND.nextDouble(-0.5, 0.5), atom.getY(), atom.getZ() + SolarisResources.RND.nextDouble(-0.5, 0.5), new ItemStack(ItemRegistries.UNSTABLE_ATOM.get("coal").get(), coalYield)));
            }

            // 用新对象写回，SynchedEntityData 才会认定为“变化”并同步给客户端。
            if (count > 0) {
                atom.setItem(atom.getItem().copyWithCount(count));
            } else {
                atom.discard();
            }
        }
    }
}
