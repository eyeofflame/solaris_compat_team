package dev.efm.solaris_compat.common.items;

import dev.efm.solaris_compat.solarisContract.SFTBQuestsAPI;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class VillagerContractItem {
    public static class EmptyContract extends Item {
        public EmptyContract() {
            super(new Properties().rarity(Rarity.UNCOMMON).stacksTo(16));
        }
    }

    public static class VillagerContract extends Item {
        public VillagerContract() {
            super(new Properties().rarity(Rarity.UNCOMMON).stacksTo(1));
        }

        @Override
        public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level pLevel, @NotNull Player pPlayer, @NotNull InteractionHand pUsedHand) {
            if (!pPlayer.level().isClientSide) {
                SFTBQuestsAPI.createFTBContract(ServerQuestFile.INSTANCE);
            }
            ItemStack stack = pPlayer.getItemInHand(pUsedHand);
            stack.shrink(1);
            return InteractionResultHolder.success(stack);
        }
    }
}
