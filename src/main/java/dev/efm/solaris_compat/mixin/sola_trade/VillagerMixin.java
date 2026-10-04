package dev.efm.solaris_compat.mixin.sola_trade;

import dev.efm.solaris_compat.events.sola_events.VillagerProfessionUpdateEvent;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerMixin {
    @Inject(method = "setVillagerData", at = @At("HEAD"))
    private void solaris$updateProfession(VillagerData newData, CallbackInfo ci) {
        Villager villager = (Villager) (Object) this;
        var old = villager.getVillagerData();

        MinecraftForge.EVENT_BUS.post(new VillagerProfessionUpdateEvent(old.getProfession(), newData.getProfession(), villager.level(), villager));
    }
}
