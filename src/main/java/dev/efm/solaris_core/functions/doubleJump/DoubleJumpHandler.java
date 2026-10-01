package dev.efm.solaris_core.functions.doubleJump;

import com.mojang.blaze3d.platform.InputConstants;
import dev.efm.solaris_core.common.effects.SMobEffectsRegister;
import dev.efm.solaris_core.common.network.DoubleJumpPacket;
import dev.efm.solaris_core.common.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.InputEvent;

public class DoubleJumpHandler {
    private static boolean conditionChecker(Player player) {
        return player.getEffect(SMobEffectsRegister.DOUBLE_JUMP.get()) != null;
    }

    private static int getLevel(Player player) {
        if (conditionChecker(player)) {
            return player.getEffect(SMobEffectsRegister.DOUBLE_JUMP.get()).getAmplifier();
        }
        return -1;
    }

    public static void handler(InputEvent.Key evt) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.onGround()) return;
        if (evt.getKey() == mc.options.keyJump.getKey().getValue() && evt.getAction() == InputConstants.PRESS) {
            PacketHandler.Instance.sendToServer(new DoubleJumpPacket(mc.player.onGround(), mc.player.getId()));
        }
    }
}
