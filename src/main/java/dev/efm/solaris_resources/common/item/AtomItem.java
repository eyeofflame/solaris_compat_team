package dev.efm.solaris_resources.common.item;

import dev.efm.solaris_resources.common.entity.AtomItemEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class AtomItem extends Item {
    public final String convertedItem;
    public final boolean isMagic;
    private final String path;

    public AtomItem(String convertedItem, boolean isMagic, String path) {
        super(new Properties().stacksTo(64));
        this.convertedItem = convertedItem;
        this.isMagic = isMagic;
        this.path = path;
    }

    @Override
    public boolean isFoil(@NotNull ItemStack pStack) {
        return isMagic;
    }

    @Override
    public @NotNull Component getName(@NotNull ItemStack pStack) {
        Component component = Component.translatable("item.solaris.magic");
        return isMagic ? Component.empty().append(Component.translatable("item.solaris_resources.atom_" + path)).append(component) : Component.empty().append(Component.translatable("item.solaris_resources.atom_" + path));
    }

    @Override
    public int getEntityLifespan(ItemStack itemStack, Level level) {
        return 1200;
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(Level level, Entity location, ItemStack stack) {
        AtomItemEntity entity = new AtomItemEntity(level,
                location.getX(), location.getY(), location.getZ(), stack);
        entity.setDeltaMovement(location.getDeltaMovement());
        return entity;
    }
}
