package dev.efm.solaris_resources.common.spells;

import com.hollingsworth.arsnouveau.api.spell.*;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.entity.AtomItemEntity;
import dev.efm.solaris_resources.common.item.AtomItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;

public class EffectAggregationAtom extends AbstractEffect {
    public EffectAggregationAtom() {
        super(
                ResourceLocation.fromNamespaceAndPath(SolarisResources.MODID, "aggregation_atom"),
                "Aggregate atom to matter."
        );
    }

    public static final EffectAggregationAtom INSTANCE = new EffectAggregationAtom();

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
            if (atom.getItem().getItem() instanceof AtomItem atomItem) {
                if (atomItem.convertedItem.isEmpty() || atomItem.ratio <= 0) continue;

                int ratio = atomItem.ratio;

                Item matter = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(atomItem.convertedItem));

                int count = atom.getItem().getCount() / ratio;

                if (count > 0) {
                    ItemEntity matterE = new ItemEntity(world, atom.getX(), atom.getY(), atom.getZ(), new ItemStack(matter, count));
                    world.addFreshEntity(matterE);
                }
                if (atom.getItem().getCount() - count * ratio <= 0) {
                    atom.discard();
                } else {
                    atom.setItem(atom.getItem().copyWithCount(atom.getItem().getCount() - count * ratio));
                }
            }
        }
    }
}
