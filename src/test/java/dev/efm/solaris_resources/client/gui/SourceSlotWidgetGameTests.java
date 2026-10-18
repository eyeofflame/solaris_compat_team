package dev.efm.solaris_resources.client.gui;

import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.lowdragmc.lowdraglib.gui.editor.annotation.LDLRegister;
import com.lowdragmc.lowdraglib.gui.editor.configurator.IConfigurableWidget;
import com.lowdragmc.lowdraglib.gui.editor.runtime.AnnotationDetector;
import dev.efm.solaris_resources.SolarisResources;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(SolarisResources.MODID)
@PrefixGameTestTemplate(false)
public class SourceSlotWidgetGameTests {

    /** 编辑器靠 LDLib 的全量注解扫描发现 widget，这里验证我们的类确实被扫到了。 */
    @GameTest(template = "empty")
    public static void widgetIsDiscoveredByEditorScan(GameTestHelper helper) {
        AnnotationDetector.Wrapper<LDLRegister, IConfigurableWidget> wrapper = null;
        for (var candidate : AnnotationDetector.REGISTER_WIDGETS) {
            if (candidate.annotation().name().equals("source_slot")) {
                wrapper = candidate;
                break;
            }
        }
        helper.assertTrue(wrapper != null, "source_slot must be discovered by LDLib's annotation scan");
        helper.assertTrue(wrapper.clazz() == SourceSlotWidget.class, "the scan must resolve our widget class");
        helper.assertTrue("widget.container".equals(wrapper.annotation().group()),
                "source_slot must land in the container tool box tab, got " + wrapper.annotation().group());

        IConfigurableWidget created = wrapper.creator().get();
        helper.assertTrue(created instanceof SourceSlotWidget, "the editor must be able to instantiate it without args");
        helper.assertTrue(created.widget().getSize().width == 18 && created.widget().getSize().height == 18,
                "the default slot must stay 18x18 like the fluid slot");
        helper.succeed();
    }

    /** 顺带守住 jar 内 UI 模板的路径：assets/<ns>/efm_ui/<name>.ui 必须能被类加载器读到并解析成 NBT。 */
    @GameTest(template = "empty")
    public static void magicCoreUiTemplateLoads(GameTestHelper helper) {
        var creator = SolarisResources.readUI(ResourceLocation.fromNamespaceAndPath(SolarisResources.MODID, "magic_core_i"));
        helper.assertTrue(creator != null, "magic_core_i.ui must be readable from the jar");
        helper.assertTrue(creator.get() != null, "the ui template must deserialize into a widget tree");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void arsTileAdapterTracksTile(GameTestHelper helper) {        FakeSourceTile tile = new FakeSourceTile();
        SourceStorage storage = SourceStorages.of(tile);
        helper.assertTrue(storage.getSource() == 0 && storage.getMaxSource() == 0, "a fresh tile must read as empty");
        tile.source = 1_234;
        tile.max = 5_000;
        helper.assertTrue(storage.getSource() == 1_234, "adapter must read the live source amount");
        helper.assertTrue(storage.getMaxSource() == 5_000, "adapter must read the live capacity");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void widgetReadsStorageOnBothSides(GameTestHelper helper) {
        FakeSourceTile tile = new FakeSourceTile();
        tile.source = 42;
        tile.max = 100;
        SourceSlotWidget widget = new SourceSlotWidget(SourceStorages.of(tile), 0, 0);
        helper.assertTrue(widget.getLastSource() == 42 && widget.getLastMaxSource() == 100,
                "construction must snapshot the storage so the server can seed the sync payload");

        tile.source = 77;
        widget.detectAndSendChanges();
        helper.assertTrue(widget.getLastSource() == 77, "server ticks must pick up the new amount");
        helper.assertTrue(widget.getLastMaxSource() == 100, "unchanged capacity must be kept");
        helper.succeed();
    }

    private static final class FakeSourceTile implements ISourceTile {
        private int source;
        private int max;

        @Override
        public int getTransferRate() {
            return 100;
        }

        @Override
        public boolean canAcceptSource() {
            return true;
        }

        @Override
        public int getSource() {
            return source;
        }

        @Override
        public int getMaxSource() {
            return max;
        }

        @Override
        public void setMaxSource(int max) {
            this.max = max;
        }

        @Override
        public int setSource(int source) {
            this.source = source;
            return source;
        }

        @Override
        public int addSource(int source) {
            this.source += source;
            return this.source;
        }

        @Override
        public int removeSource(int source) {
            this.source -= source;
            return this.source;
        }
    }
}
