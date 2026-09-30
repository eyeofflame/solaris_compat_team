package dev.efm.solaris_resources.common.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class AtomItemEntity extends ItemEntity {

    public AtomItemEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
        this.setNeverPickUp();
    }

    public AtomItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        super(level, x, y, z, stack);
        this.setNeverPickUp();
    }

    @Override
    public void playerTouch(@NotNull Player player) {
    }
}
