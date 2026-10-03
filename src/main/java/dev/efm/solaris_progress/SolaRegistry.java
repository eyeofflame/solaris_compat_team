package dev.efm.solaris_progress;

import dev.efm.solaris_progress.worldgen.FlatBlendDensityFunction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

/**
 * 把运行期包裹用的 {@link FlatBlendDensityFunction} 注册进密度函数类型表，
 * 让 NoiseRouter 的序列化路径能正常编解码，而不是抛异常。
 */
@Mod.EventBusSubscriber(modid = SolaProgress.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SolaRegistry {
    private SolaRegistry() {}

    @SubscribeEvent
    public static void onRegister(RegisterEvent event) {
        if (!event.getRegistryKey().equals(Registries.DENSITY_FUNCTION_TYPE)) return;
        event.register(Registries.DENSITY_FUNCTION_TYPE,
                ResourceLocation.fromNamespaceAndPath(SolaProgress.MODID, "flat_blend"),
                () -> FlatBlendDensityFunction.CODEC.codec());
    }
}
