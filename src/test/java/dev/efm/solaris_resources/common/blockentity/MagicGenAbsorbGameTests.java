package dev.efm.solaris_resources.common.blockentity;

import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.SwitchWidget;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.registration.BlockRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

@GameTestHolder(SolarisResources.MODID)
@PrefixGameTestTemplate(false)
public class MagicGenAbsorbGameTests {

    /** 抽干玩家魔力并按 1:1 转成魔源。 */
    @GameTest(template = "empty")
    public static void absorbDrainsPlayerManaIntoSource(GameTestHelper helper) {
        MagicGenEntity core = placeMachine(helper, new BlockPos(2, 2, 2));
        Player player = helper.makeMockPlayer();
        IManaCap mana = manaOf(player);
        mana.setMaxMana(1000);
        mana.setMana(300);

        int added = core.absorbMana(List.of(player));

        helper.assertTrue(added == 300, "300 mana must convert into 300 source, got " + added);
        helper.assertTrue(core.getSource() == 300, "the machine must store the converted source");
        helper.assertTrue(core.getMaxSource() == MagicGenEntity.DEFAULT_MAX_SOURCE, "the 800 cap must stay");
        helper.assertTrue(mana.getCurrentMana() == 0, "the player's mana must be drained");
        helper.succeed();
    }

    /** 魔源只剩 100 空间时，玩家的 500 魔力仍然被全部抽走，只转化得下的部分。 */
    @GameTest(template = "empty")
    public static void absorbClampsAtCapacity(GameTestHelper helper) {
        MagicGenEntity core = placeMachine(helper, new BlockPos(2, 2, 2));
        core.setSource(700);
        Player player = helper.makeMockPlayer();
        IManaCap mana = manaOf(player);
        mana.setMaxMana(1000);
        mana.setMana(500);

        int added = core.absorbMana(List.of(player));

        helper.assertTrue(added == 100, "only the remaining 100 space may be filled, got " + added);
        helper.assertTrue(core.getSource() == MagicGenEntity.DEFAULT_MAX_SOURCE, "the machine must end up full");
        helper.assertTrue(mana.getCurrentMana() == 0, "the player is still fully drained by design");
        helper.succeed();
    }

    /** 已经满的机器不会再去抽玩家。 */
    @GameTest(template = "empty")
    public static void fullMachineDrainsNobody(GameTestHelper helper) {
        MagicGenEntity core = placeMachine(helper, new BlockPos(2, 2, 2));
        core.setSource(MagicGenEntity.DEFAULT_MAX_SOURCE);
        Player player = helper.makeMockPlayer();
        IManaCap mana = manaOf(player);
        mana.setMaxMana(1000);
        mana.setMana(400);

        helper.assertTrue(core.absorbMana(List.of(player)) == 0, "a full machine must not absorb");
        helper.assertTrue(mana.getCurrentMana() == 400, "a full machine must leave the player's mana alone");
        helper.succeed();
    }

    /** 吸取开关要能过存档往返。 */
    @GameTest(template = "empty")
    public static void absorbingFlagSurvivesSave(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        BlockPos absolute = helper.absolutePos(pos);
        MagicGenEntity core = placeMachine(helper, pos);
        helper.assertFalse(core.isAbsorbing(), "absorption must default to off");
        core.setAbsorbing(true);
        core.setSource(120);

        CompoundTag saved = core.saveWithoutMetadata();
        MagicGenEntity restored = BlockRegistries.MAGIC_GEN_ENTITY.get()
                .create(absolute, helper.getLevel().getBlockState(absolute));
        helper.assertTrue(restored != null, "the registered block entity type must create a machine");
        restored.setLevel(helper.getLevel());
        restored.load(saved);

        helper.assertTrue(restored.isAbsorbing(), "the absorption toggle must round-trip");
        helper.assertTrue(restored.getSource() == 120, "source must still round-trip alongside the toggle");
        helper.succeed();
    }

    /** UI 模板里那个开关必须存在，并且确实是 LDLib 的 SwitchWidget。 */
    @GameTest(template = "empty")
    public static void absorbSwitchIsPresent(GameTestHelper helper) {
        MagicGenEntity core = placeMachine(helper, new BlockPos(2, 2, 2));
        ModularUI ui = core.createUI(helper.makeMockPlayer());
        var widget = ui.mainGroup.getFirstWidgetById(MagicGenEntity.ABSORB_BUTTON_ID);
        helper.assertTrue(widget instanceof SwitchWidget,
                "magic_core_i.ui must expose " + MagicGenEntity.ABSORB_BUTTON_ID + " as a switch, got " + widget);
        helper.succeed();
    }

    private static MagicGenEntity placeMachine(GameTestHelper helper, BlockPos pos) {
        helper.setBlock(pos, BlockRegistries.MAGIC_GEN_MACHINE.get());
        MagicGenEntity core = (MagicGenEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        if (core == null) {
            throw new IllegalStateException("the machine block entity must exist");
        }
        return core;
    }

    private static IManaCap manaOf(Player player) {
        IManaCap mana = CapabilityRegistry.getMana(player).orElse(null);
        if (mana == null) {
            throw new IllegalStateException("Ars Nouveau's mana capability must attach to players");
        }
        return mana;
    }
}
