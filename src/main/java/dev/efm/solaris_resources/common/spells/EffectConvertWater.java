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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public class EffectConvertWater extends AbstractEffect {
    public EffectConvertWater() {
        super(
                ResourceLocation.fromNamespaceAndPath(SolarisResources.MODID, "convert_water"),
                "Convert Water to Crystallized Water"
        );
    }

    public static final EffectConvertWater INSTANCE = new EffectConvertWater();

    @Override
    protected int getDefaultManaCost() {
        return 30;
    }

    @Override
    protected @NotNull Set<AbstractAugment> getCompatibleAugments() {
        return Set.of();
    }

    @Override
    public void onResolveBlock(BlockHitResult rayTraceResult, Level world, @NotNull LivingEntity shooter, SpellStats spellStats, SpellContext spellContext, SpellResolver resolver) {
        if (!world.isClientSide) {
            BlockPos pos = rayTraceResult.getBlockPos();
            BlockPos pos0 = new BlockPos(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1);
            BlockPos pos1 = new BlockPos(pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);

            for (BlockPos position : BlockPos.betweenClosed(pos0, pos1)) {
                if (world.getBlockState(position).getBlock().equals(Blocks.WATER)) {
                    world.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
                    ItemEntity entity = new ItemEntity(world, position.getX() * 1d, position.getY() * 1d, position.getZ() * 1d, new ItemStack(ItemRegistries.CRYSTALLIZED_WATER.get(), 4));
                    world.addFreshEntity(entity);
                }
            }
        }
    }
}
