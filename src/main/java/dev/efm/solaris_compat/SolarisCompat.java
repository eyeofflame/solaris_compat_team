package dev.efm.solaris_compat;

import com.lowdragmc.lowdraglib.gui.factory.UIFactory;
import dev.efm.solaris_compat.command.SolaExportCommand;
import dev.efm.solaris_compat.common.SRegistry;
import dev.efm.solaris_compat.config.ConfigScreen;
import dev.efm.solaris_compat.config.SolarisConfig;
import dev.efm.solaris_compat.data.DataRegistry;
import dev.efm.solaris_compat.data.TradeData;
import dev.efm.solaris_compat.data.reader.TradeConfigLoader;
import dev.efm.solaris_compat.events.sola_events.VillagerProfessionUpdateEvent;
import dev.efm.solaris_compat.ldlib.SolaTradeFactory;
import dev.efm.solaris_compat.ldlib.SolaTradeHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(SolarisCompat.MODID)
public class SolarisCompat {
    public static final String MODID = "solaris_compat";

    public SolarisCompat(FMLJavaModLoadingContext context) {
        context.registerConfig(ModConfig.Type.COMMON, SolarisConfig.SPEC);

        context.registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, screen) -> ConfigScreen.create(screen)
                )
        );

        IEventBus ibus = context.getModEventBus(), fbus = MinecraftForge.EVENT_BUS;

        ibus.addListener(DataRegistry::DataRegistryEvent);
        ibus.addListener(DataRegistry::GatherDataEvent);

        SRegistry.register(ibus);

        fbus.addListener(this::onVillagerProUpdate);
        fbus.addListener(SolaExportCommand::register);
        fbus.addListener(this::onClickVillager);

        UIFactory.register(SolaTradeFactory.INSTANCE);

    }


    //custom event
    public void onVillagerProUpdate(VillagerProfessionUpdateEvent event) {
        if (event.getLevel().isClientSide) return;

        VillagerProfession oldP = event.getOld(), newP = event.getNew();
        if (oldP.equals(VillagerProfession.NONE) && !newP.equals(VillagerProfession.NONE)) {
            // 用注册表 key 而不是 toString()：VillagerProfession.toString() 只有裸路径名，
            // mod 职业会丢命名空间
            ResourceLocation id = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(newP);
            TradeData data = id == null ? null : TradeConfigLoader.get(id);

            if (data != null) {
                Villager villager = event.getVillager();
                // 在这里落定 pick：每级从 pool 随机抽 pick 条，抽签结果写进村民 NBT，
                // 客户端只负责展示（不会自己算）
                villager.getPersistentData().put("sola_trades", data.picked(villager.getRandom()).toNbt());
            }
        } else if (!oldP.equals(VillagerProfession.NONE) && newP.equals(VillagerProfession.NONE)) {
            Villager villager = event.getVillager();
            villager.getPersistentData().remove("sola_trades");
        }
    }

    public void onClickVillager(PlayerInteractEvent.EntityInteract event) {
        if (event.getTarget() instanceof Villager villager && !event.getEntity().level().isClientSide) {
            ServerPlayer player = (ServerPlayer) event.getEntity();

            // 以村民身上的快照为准：职业设定时已按 pick 抽定结果，客户端只展示；
            // 没有快照的老村民（或空壳快照）现场按配置抽一份补上
            TradeData tradeData = TradeData.fromNbt(villager.getPersistentData().getCompound("sola_trades"));
            if (tradeData.getTrades().isEmpty()) {
                ResourceLocation id = ForgeRegistries.VILLAGER_PROFESSIONS.getKey(villager.getVillagerData().getProfession());
                TradeData fromConfig = id == null ? null : TradeConfigLoader.get(id);
                if (fromConfig != null) {
                    tradeData = fromConfig.picked(villager.getRandom());
                    villager.getPersistentData().put("sola_trades", tradeData.toNbt());
                }
            }
            SolaTradeHolder holder = new SolaTradeHolder(tradeData);

            if (event.getItemStack().equals(ItemStack.EMPTY)) SolaTradeFactory.INSTANCE.openUI(holder, player);
            event.setCanceled(true);
        }
    }
}
