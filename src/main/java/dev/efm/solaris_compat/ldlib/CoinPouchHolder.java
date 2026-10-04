package dev.efm.solaris_compat.ldlib;

import com.lowdragmc.lowdraglib.LDLib;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import dev.efm.solaris_compat.common.SRegistry;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * 硬币袋界面的持有者：只携带「硬币袋在哪」这一个定位信息，物品本身每次现取——Curios 与
 * 原版的同步会**整只替换**已装备/背包里的 ItemStack 对象，任何缓存都会过期。
 *
 * <p>同步数据里只写 {@link Source}；玩家对象在 {@code createUITemplate} 时挂上（服务端是
 * ServerPlayer，客户端是 LocalPlayer），所以客户端不需要碰任何客户端专属类。
 */
public final class CoinPouchHolder implements IUIHolder {

    public enum Source { HAND_MAIN, HAND_OFF, CURIO }

    /** 饰品槽 id，与 data/solaris_compat/curios/slots/coin_pouch.json 的文件名一致。 */
    public static final String SLOT_ID = "coin_pouch";

    private final Source source;
    private Player player;

    public CoinPouchHolder(Source source) {
        this.source = source;
    }

    public Source source() {
        return source;
    }

    /** createUITemplate 两端都会带着玩家调用，在这里挂上。 */
    public void attachPlayer(Player player) {
        this.player = player;
    }

    public ItemStack resolve(Player player) {
        return switch (source) {
            case HAND_MAIN -> player.getMainHandItem();
            case HAND_OFF -> player.getOffhandItem();
            case CURIO -> CuriosApi.getCuriosInventory(player).resolve()
                    .flatMap(handler -> handler.getStacksHandler(SLOT_ID))
                    .map(handler -> handler.getStacks().getStackInSlot(0))
                    .orElse(ItemStack.EMPTY);
        };
    }

    /** 当前硬币袋物品；解析不到（被移走/换手）时为空栈。 */
    public ItemStack pouch() {
        return player == null ? ItemStack.EMPTY : resolve(player);
    }

    public boolean hasPouch() {
        ItemStack pouch = pouch();
        return !pouch.isEmpty() && pouch.is(SRegistry.COIN_POUCH.get());
    }

    @Override
    public ModularUI createUI(Player entityPlayer) {
        return CoinPouchFactory.INSTANCE.createUITemplate(this, entityPlayer);
    }

    @Override
    public boolean isInvalid() {
        // 界面开着时硬币袋被移走/换掉 → LDLib 据此自动关闭容器
        return player != null && (player.isRemoved() || !hasPouch());
    }

    @Override
    public boolean isRemote() {
        return LDLib.isRemote();
    }

    @Override
    public void markAsDirty() {
    }
}
