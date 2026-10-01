package dev.efm.solaris_core.common.network;

import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.modular.ModularUIGuiContainer;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import dev.efm.solaris_core.SolarisCore;
import dev.efm.solaris_core.functions.backToDeath.PlayerPositionData;
import dev.efm.solaris_core.functions.backToDeath.menuHelper.SMenuAPI;
import dev.efm.solaris_core.helper.GameHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class UIPacket {
    private final int id;
    private final List<PlayerPositionData> list;
    private final float choose;

    public UIPacket(int Id, List<PlayerPositionData> data, float chosen) {
        this.id = Id;
        this.list = data;
        this.choose = chosen;
    }

    public static void encode(UIPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.id);
        ListTag listTag = new ListTag();
        for (int i = 0; i < packet.list.size(); i++) {
            PlayerPositionData data = packet.list.get(i);

            CompoundTag tag = new CompoundTag();
            tag.putDouble("x", data.position.x);
            tag.putDouble("y", data.position.y);
            tag.putDouble("z", data.position.z);
            tag.putString("name", data.dimension.toString());

            listTag.add(i, tag);
        }

        CompoundTag tag = new CompoundTag();
        tag.put("data_list", listTag);

        buf.writeNbt(tag);

        buf.writeFloat(packet.choose);
    }

    public static UIPacket decode(FriendlyByteBuf buf) {
        int id0 = buf.readInt();
        ListTag listTag = (ListTag) buf.readNbt().get("data_list");
        List<PlayerPositionData> dataList = new ArrayList<>();

        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag tag = (CompoundTag) listTag.get(i);
            double x = tag.getDouble("x");
            double y = tag.getDouble("y");
            double z = tag.getDouble("z");
            String name = tag.getString("name");

            Vec3 position = new Vec3(x, y, z);
            ResourceLocation resourceLocation = GameHelper.buildRes(name, ':');

            dataList.add(i, new PlayerPositionData(position, resourceLocation));
        }

        float cho0 = buf.readFloat();

        return new UIPacket(id0, dataList, cho0);
    }

    public static void handle(UIPacket packet, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isClient()) {
                Minecraft mc = Minecraft.getInstance();
                LocalPlayer player = mc.player;
                if (player != null && player.getId() == packet.id && packet.choose == -1f) {
                    ModularUI ui = new ModularUI(SMenuAPI.createDListGUI(mc.getWindow().getScreenWidth(), mc.getWindow().getScreenHeight(), mc.getWindow().getGuiScale(), packet.list), IUIHolder.EMPTY, player);
                    ui.initWidgets();
                    ModularUIGuiContainer container = new ModularUIGuiContainer(ui, player.containerMenu.containerId);
                    mc.setScreen(container);
                }
            } else if (context.getDirection().getReceptionSide().isServer() && packet.choose >= 0f) {
                ServerPlayer player = context.getSender();
                if (player.getId() == packet.id) {
                    int id = (int) packet.choose;
                    PlayerPositionData data = packet.list.get(id);
                    ResourceKey<Level> resourceKey = ResourceKey.create(Registries.DIMENSION, data.dimension);
                    ServerLevel serverLevel = player.server.getLevel(resourceKey);

                    player.teleportTo(serverLevel, data.position.x, data.position.y, data.position.z, player.getYRot(), player.getXRot());
                    player.sendSystemMessage(Component.translatable("tip.solaris_core.death_command_sucess").append(String.format("%s %.1f , %.1f , %.1f", data.dimension.toString(), data.position.x, data.position.y, data.position.z)).withStyle(ChatFormatting.AQUA));
                }
            }
        });

        context.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    public static void WindowChangeEvent(ScreenEvent.Init evt) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;

        if (evt.getScreen() instanceof ModularUIGuiContainer container) {
            Widget root = container.modularUI.getFirstWidgetById(SolarisCore.MODID + ":DListGUI");
            if (root != null) {

                int x = (container.modularUI.getScreenWidth() - root.getSize().width) / 2;
                int y = (container.modularUI.getScreenHeight() - root.getSize().height) / 2;

                root.setSelfPosition(x, y);


            }
        }
    }
}
