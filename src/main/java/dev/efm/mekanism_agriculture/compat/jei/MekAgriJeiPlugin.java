package dev.efm.mekanism_agriculture.compat.jei;

import com.blakebr0.mysticalagriculture.compat.jei.category.InfusionCategory;
import dev.efm.mekanism_agriculture.MekanismAgriculture;
import dev.efm.mekanism_agriculture.client.GuiMekInfusioner;
import dev.efm.mekanism_agriculture.common.registration.MekBlocks;
import mekanism.client.gui.element.progress.ProgressType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

/**
 * JEI 联动。
 *
 * <p>这台机器跑的就是神秘农业的注魔配方,所以<b>不另起类别</b> —— 直接把机器登记成神秘农业「注魔」
 * 类别的催化剂。这样玩家在 JEI 里点机器、或在机器 GUI 里按 R/U,看到的都是同一套注魔配方,
 * 和注魔祭坛完全一致;物品的「用途」页里也会带上这台机器。
 *
 * <p>引用 {@link InfusionCategory} 是安全的:JEI 的 {@code IModPlugin} 只在客户端加载
 * (神秘农业自己也在这个类里直接引用客户端 Screen 类)。
 */
@JeiPlugin
public class MekAgriJeiPlugin implements IModPlugin {

    @NotNull
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(MekanismAgriculture.MODID, "jei");
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(MekBlocks.MEK_INFUSIONER.getItemStack(), InfusionCategory.RECIPE_TYPE);
    }

    @Override
    public void registerGuiHandlers(@NotNull IGuiHandlerRegistration registration) {
        // 尺寸直接问 ProgressType 要,免得 Mekanism 改了进度条大小而这里的点击区不同步
        registration.addRecipeClickArea(GuiMekInfusioner.class,
                GuiMekInfusioner.PROGRESS_X, GuiMekInfusioner.PROGRESS_Y,
                ProgressType.BAR.getWidth(), ProgressType.BAR.getHeight(),
                InfusionCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipeTransferHandlers(@NotNull IRecipeTransferRegistration registration) {
        // 9 个输入槽手摆太烦,让 JEI 的「+」一键从背包填料
        registration.addRecipeTransferHandler(new MekAgriRecipeTransferInfo());
    }
}
