package dev.efm.solaris_compat.events.sola_events;

import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.level.Level;
import net.minecraftforge.eventbus.api.Event;

public class VillagerProfessionUpdateEvent extends Event {
    protected final VillagerProfession oldProfession;
    protected final VillagerProfession newProfession;
    protected final Level level;
    protected final Villager villager;

    public VillagerProfessionUpdateEvent(VillagerProfession oldProfession, VillagerProfession newProfession, Level level, Villager villager) {
        this.oldProfession = oldProfession;
        this.newProfession = newProfession;
        this.level = level;
        this.villager = villager;
    }

    public VillagerProfession getOld() {
        return oldProfession;
    }

    public VillagerProfession getNew() {
        return newProfession;
    }

    public Level getLevel() {
        return this.level;
    }

    public Villager getVillager() {
        return this.villager;
    }
}
