package dev.efm.solaris_core.common.capabilities;

import dev.efm.solaris_core.SolarisCore;
import dev.efm.solaris_core.functions.backToDeath.IPlayerDListCap;
import dev.efm.solaris_core.functions.backToDeath.PlayerDListCap;
import dev.efm.solaris_core.functions.backToDeath.PlayerPositionData;
import dev.efm.solaris_core.functions.resProc.IPlayerDurData;
import dev.efm.solaris_core.functions.resProc.PlayerDurData;
import dev.efm.solaris_core.helper.GameHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class SolarisCapabilities {
    public static final Capability<IPlayerDurData> PLAYER_DUR_DATA_CAPABILITY = CapabilityManager.get(new CapabilityToken<IPlayerDurData>() {
    });
    public static final Capability<IPlayerDListCap> PLAYER_D_LIST_CAP_CAPABILITY = CapabilityManager.get(new CapabilityToken<IPlayerDListCap>() {
    });

    public static void registerCapa(RegisterCapabilitiesEvent event) {
        event.register(PlayerDurDataProvider.class);
        event.register(PlayerDListProvider.class);
    }

    public static void onAttach(AttachCapabilitiesEvent<Entity> evt) {
        if (evt.getObject() instanceof ServerPlayer) {
            evt.addCapability(GameHelper.buildRes(SolarisCore.MODID, "player_dur_data"), new PlayerDurDataProvider());
            evt.addCapability(GameHelper.buildRes(SolarisCore.MODID, "player_death_list_data"), new PlayerDListProvider());
        }
    }

    private static class PlayerDurDataProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final IPlayerDurData data = new PlayerDurData();
        private final LazyOptional<IPlayerDurData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == PLAYER_DUR_DATA_CAPABILITY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("dur_time", data.getValue());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            data.setValue(nbt.getInt("dur_time"));
        }
    }

    private static class PlayerDListProvider implements INBTSerializable<CompoundTag>, ICapabilityProvider {
        private final IPlayerDListCap data = new PlayerDListCap();
        private final LazyOptional<IPlayerDListCap> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == PLAYER_D_LIST_CAP_CAPABILITY ? optional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            ListTag listTag = new ListTag();
            for (PlayerPositionData value : data.getDList()) {
                CompoundTag vTag = new CompoundTag();
                vTag.putDouble("x", value.position.x);
                vTag.putDouble("y", value.position.y);
                vTag.putDouble("z", value.position.z);
                vTag.putString("dimension", value.dimension.toString());
                listTag.add(vTag);
            }
            tag.put("death_list", listTag);
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            ListTag listTag = nbt.getList("death_list", Tag.TAG_COMPOUND);
            List<PlayerPositionData> vList = new ArrayList<>();
            for (int i = 0; i < listTag.size(); i++) {
                CompoundTag vTag = listTag.getCompound(i);
                double x = vTag.getDouble("x");
                double y = vTag.getDouble("y");
                double z = vTag.getDouble("z");
                String dimension = vTag.getString("dimension");
                vList.add(new PlayerPositionData(new Vec3(x, y, z), ResourceLocation.bySeparator(dimension, ':')));
            }
            data.setDList(vList);
        }
    }
}
