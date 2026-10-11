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
        List<AtomItemEntity> atoms = world.getEntitiesOfClass(AtomItemEntity.class, SpellEffectRange.ABOVE.area(origin));

        for (AtomItemEntity atom : atoms) {
            ItemStack stack = atom.getItem();
            if (stack.getItem() instanceof AtomItem atomItem) {
                var settings = atomItem.getConversionSettings();
                if (settings.convertedItem().isEmpty() || settings.ratio() <= 0) continue;

                int ratio = settings.ratio();

                Item matter = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(settings.convertedItem()));
                if (matter == null) continue;

                int count = stack.getCount() / ratio;

                if (count > 0) {
                    ItemStack output = new ItemStack(matter);
                    for (int remaining = count; remaining > 0; ) {
                        int stackSize = Math.min(remaining, output.getMaxStackSize());
                        ItemEntity matterE = new ItemEntity(world, atom.getX(), atom.getY(), atom.getZ(),
                                output.copyWithCount(stackSize));
                        world.addFreshEntity(matterE);
                        remaining -= stackSize;
                    }
                }
                if (stack.getCount() - count * ratio <= 0) {
                    atom.discard();
                } else {
                    atom.setItem(stack.copyWithCount(stack.getCount() - count * ratio));
                }
            }
        }
    }
}
