package dev.efm.solaris_resources.common.blockentity;

import com.hollingsworth.arsnouveau.common.block.tile.RelayTile;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.common.registration.BlockRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

@GameTestHolder(SolarisResources.MODID)
@PrefixGameTestTemplate(false)
public class SourceRelayGameTests {

    private static final ResourceLocation RELAY = ResourceLocation.fromNamespaceAndPath("ars_nouveau", "relay");

    /**
     * 支配之杖的第二次点击：以中继器的身份绑定目标。
     * 回归点——中继器原实现只认 AbstractSourceMachine，会把我们的机器判成"不可连接"。
     */
    @GameTest(template = "empty")
    public static void dominionWandCanBindRelayToMachine(GameTestHelper helper) {
        BlockPos relayPos = new BlockPos(2, 2, 2);
        BlockPos corePos = new BlockPos(4, 2, 2);
        BlockPos coreAbsolute = helper.absolutePos(corePos);

        helper.setBlock(relayPos, relay());
        helper.setBlock(corePos, BlockRegistries.MAGIC_GEN_MACHINE.get());

        RelayTile relay = (RelayTile) helper.getLevel().getBlockEntity(helper.absolutePos(relayPos));
        helper.assertTrue(relay != null, "the relay block entity must exist");

        // 这就是 DominionWand#useOn 在第二次点击时做的调用。
        relay.onFinishedConnectionFirst(coreAbsolute, null, helper.makeMockPlayer());

        helper.assertTrue(coreAbsolute.equals(relay.getToPos()),
                "the relay must accept the machine as a send target, got " + relay.getToPos());
        helper.assertTrue(relay.setSendTo(coreAbsolute), "setSendTo must accept the machine directly too");
        helper.succeed();
    }

    /** 绑定之后，中继器每 20 tick 送一次魔源，机器必须真的收到。 */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void relayPushesSourceIntoMachine(GameTestHelper helper) {
        BlockPos relayPos = new BlockPos(2, 2, 2);
        BlockPos corePos = new BlockPos(4, 2, 2);
        BlockPos coreAbsolute = helper.absolutePos(corePos);

        helper.setBlock(relayPos, relay());
        helper.setBlock(corePos, BlockRegistries.MAGIC_GEN_MACHINE.get());

        RelayTile relay = (RelayTile) helper.getLevel().getBlockEntity(helper.absolutePos(relayPos));
        MagicGenEntity core = (MagicGenEntity) helper.getLevel().getBlockEntity(coreAbsolute);
        helper.assertTrue(relay != null && core != null, "both block entities must exist");
        relay.onFinishedConnectionFirst(coreAbsolute, null, helper.makeMockPlayer());
        helper.assertTrue(coreAbsolute.equals(relay.getToPos()), "binding must succeed before transfer");
        relay.setSource(500);

        helper.startSequence()
                .thenIdle(45)
                .thenExecute(() -> helper.assertTrue(core.getSource() > 0,
                        "the relay must push source into the machine, got " + core.getSource()))
                .thenSucceed();
    }

    private static Block relay() {
        Block relay = ForgeRegistries.BLOCKS.getValue(RELAY);
        if (relay == null) {
            throw new IllegalStateException("ars_nouveau:relay is missing; the integration cannot be tested");
        }
        return relay;
    }
}
