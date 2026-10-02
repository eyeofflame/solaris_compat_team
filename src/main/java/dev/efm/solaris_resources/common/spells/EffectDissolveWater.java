package dev.efm.solaris_resources.common.spells;

import com.hollingsworth.arsnouveau.api.spell.*;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.registration.ItemRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class EffectDissolveWater extends AbstractEffect {
    public EffectDissolveWater() {
        super(
                ResourceLocation.fromNamespaceAndPath(SolarisResources.MODID, "dissolve_water"),
                "Dissolve Water into atom"
        );
    }

    public static final EffectDissolveWater INSTANCE = new EffectDissolveWater();

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
        if (!world.isClientSide) {
            BlockPos origin = rayTraceResult.getBlockPos();
            var atoms = world.getEntitiesOfClass(ItemEntity.class, SpellEffectRange.ABOVE.area(origin));

            for (ItemEntity itemEntity : atoms) {
                if (itemEntity.getItem().getItem().equals(ItemRegistries.CRYSTALLIZED_WATER.get())) {
                    int count = itemEntity.getItem().getCount();
                    ItemEntity hydro = new ItemEntity(world, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(SolarisResources.MODID, "unstable_magic_atom_hydrogen")), 16 * count));
                    ItemEntity oxy = new ItemEntity(world, itemEntity.getX(), itemEntity.getY(), itemEntity.getZ(), new ItemStack(ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(SolarisResources.MODID, "unstable_magic_atom_oxygen")), 8 * count));

                    world.addFreshEntity(hydro);
                    world.addFreshEntity(oxy);
                    itemEntity.discard();
                }
            }

        }
    }
}
