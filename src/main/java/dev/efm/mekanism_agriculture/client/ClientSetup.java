package dev.efm.mekanism_agriculture.client;

import dev.efm.mekanism_agriculture.common.registration.MekContainerTypes;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientSetup {

    public static void clientSetup(FMLClientSetupEvent evt) {
        evt.enqueueWork(() -> MenuScreens.register(MekContainerTypes.MEK_INFUSIONER.get(), GuiMekInfusioner::new));
    }
}
