package dev.efm.rpg;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import dev.efm.rpg.data.Script;
import dev.efm.rpg.widgets.DialogueRoot;
import dev.efm.solaris_compat.SolarisCompat;
import dev.efm.solaris_compat.api.SHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Objects;

/**
 * 剧本 UI 的工厂。整棵控件树由 {@link DialogueRoot} 负责组装，
 * 这里只做"把剧本同步到客户端 + 开界面"。
 */
public class SFactory extends UIFactory<SHolder> {
    public static final SFactory INSTANCE = new SFactory();

    public SFactory() {
        super(SHelper.buildRes(SolarisCompat.MODID, "gui"));
    }

    @Override
    protected ModularUI createUITemplate(SHolder holder, Player entityPlayer) {
        if (holder == null || holder.script() == null) {
            return null;
        }
        // 双参构造 = fullScreen，DialogueRoot 会在 onScreenSizeUpdate 里自己撑满屏幕
        return new ModularUI(holder, entityPlayer).widget(new DialogueRoot(holder.script()));
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    protected SHolder readHolderFromSyncData(FriendlyByteBuf syncData) {
        return new SHolder(Script.deserialize(Objects.requireNonNull(syncData.readNbt())));
    }

    @Override
    protected void writeHolderToSyncData(FriendlyByteBuf syncData, SHolder holder) {
        syncData.writeNbt(holder.script().serialize());
    }
}
