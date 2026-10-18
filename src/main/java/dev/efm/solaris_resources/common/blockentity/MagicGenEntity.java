package dev.efm.solaris_resources.common.blockentity;

import com.hollingsworth.arsnouveau.api.mana.IManaCap;
import com.hollingsworth.arsnouveau.api.source.ISourceTile;
import com.hollingsworth.arsnouveau.common.network.Networking;
import com.hollingsworth.arsnouveau.common.network.PacketUpdateMana;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import com.lowdragmc.lowdraglib.gui.modular.IUIHolder;
import com.lowdragmc.lowdraglib.gui.modular.ModularUI;
import com.lowdragmc.lowdraglib.gui.widget.*;
import com.lowdragmc.lowdraglib.misc.ItemStackTransfer;
import com.lowdragmc.lowdraglib.side.fluid.FluidTransferHelper;
import com.lowdragmc.lowdraglib.side.fluid.IFluidTransfer;
import com.lowdragmc.lowdraglib.side.item.IItemTransfer;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import dev.efm.solaris_resources.SolarisResources;
import dev.efm.solaris_resources.client.gui.SourceSlotWidget;
import dev.efm.solaris_resources.client.gui.SourceStorages;
import dev.efm.solaris_resources.common.block.MagicGenMachine;
import dev.efm.solaris_resources.common.config.MachineConfigStore;
import dev.efm.solaris_resources.common.config.MachineConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public class MagicGenEntity extends KineticBlockEntity implements GeoBlockEntity, IUIHolder.BlockEntityUI, ISourceTile {

    public static final ResourceLocation MAGIC_CORE_UI = ResourceLocation.fromNamespaceAndPath(SolarisResources.MODID, "magic_core_i");

    /**
     * 魔源上限：默认 800，存档会记下来（保留 setMaxSource 以便以后接配置）。
     */
    public static final int DEFAULT_MAX_SOURCE = 800;

    /**
     * 编辑器里那个按钮的 id。
     */
    public static final String ABSORB_BUTTON_ID = "sola_button_0";

    /**
     * 编辑器里进度条的 id。
     */
    public static final String PROGRESS_ID = "sola_progress";

    /**
     * 吸取节拍与范围：每 20 tick、以机器为中心的 5×5×5。
     */
    public static final int ABSORB_INTERVAL = 20;
    public static final double ABSORB_RANGE = 5.0D;

    private static final String SOURCE_TAG = "Source";
    private static final String MAX_SOURCE_TAG = "MaxSource";
    private static final String ABSORBING_TAG = "Absorbing";
    private static final String PROGRESS_TAG = "Progress";
    private static final String COMPLETED_TAG = "CompletedProcesses";
    private static final String OUTPUTS_TAG = "Outputs";

    private final AnimatableInstanceCache animatableCache = GeckoLibUtil.createInstanceCache(this);

    private int source;
    private int maxSource = DEFAULT_MAX_SOURCE;
    private boolean absorbing;

    public MagicGenEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(typeIn, pos, state);
    }

    // 应力冲击不在这里写死：KineticBlockEntity 的默认实现读 BlockStressValues，
    // 与 BlockRegistries.registerStressImpact 注册的那一份保持单一来源。

    // ------------------------------------------------------------ 魔源存储（ISourceTile）

    /**
     * 与魔源罐一致：一次搬运就能填满/清空本机。
     */
    @Override
    public int getTransferRate() {
        return maxSource;
    }

    @Override
    public boolean canAcceptSource() {
        return source < maxSource;
    }

    @Override
    public int getSource() {
        return source;
    }

    @Override
    public int getMaxSource() {
        return maxSource;
    }

    @Override
    public void setMaxSource(int max) {
        this.maxSource = Math.max(0, max);
        if (source > maxSource) {
            this.source = maxSource;
        }
        onStateChanged();
    }

    @Override
    public int setSource(int value) {
        int clamped = Mth.clamp(value, 0, maxSource);
        if (clamped != source) {
            this.source = clamped;
            onStateChanged();
        }
        return source;
    }

    @Override
    public int addSource(int amount) {
        if (amount <= 0) {
            return source;
        }
        return setSource((int) Math.min((long) source + amount, maxSource));
    }

    @Override
    public int removeSource(int amount) {
        if (amount <= 0) {
            return source;
        }
        return setSource((int) Math.max((long) source - amount, 0));
    }

    // ------------------------------------------------------------ 玩家魔力吸取

    public boolean isAbsorbing() {
        return absorbing;
    }

    public void setAbsorbing(boolean value) {
        if (this.absorbing == value) {
            return;
        }
        this.absorbing = value;
        onStateChanged();
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide) {
            return;
        }
        if (absorbing && level.getGameTime() % ABSORB_INTERVAL == 0) {
            AABB area = AABB.ofSize(Vec3.atCenterOf(worldPosition), ABSORB_RANGE, ABSORB_RANGE, ABSORB_RANGE);
            absorbMana(level.getEntitiesOfClass(Player.class, area));
        }
        tickProcessing();
    }

    /**
     * 抽干这批玩家的魔力并按 1:1 转成魔源；魔源满了就停手，不再继续抽人。
     * 单次吸取超过剩余容量的部分会被丢弃（"全部魔力"优先于"不浪费"）。
     *
     * @return 实际增加的魔源量
     */
    public int absorbMana(Iterable<? extends Player> players) {
        if (maxSource - source <= 0) {
            return 0;
        }
        int added = 0;
        for (Player player : players) {
            int space = maxSource - source;
            if (space <= 0) {
                break;
            }
            IManaCap mana = CapabilityRegistry.getMana(player).orElse(null);
            if (mana == null) {
                continue;
            }
            double current = mana.getCurrentMana();
            if (current <= 0) {
                continue;
            }
            mana.setMana(0);
            syncMana(player, mana);
            int gain = (int) Math.min((long) current, space);
            if (gain > 0) {
                addSource(gain);
                added += gain;
            }
        }
        return added;
    }

    /**
     * AN 的魔力是客户端缓存，抽干后要主动同步一次，否则玩家的魔力条会停在旧值。
     */
    private static void syncMana(Player player, IManaCap mana) {
        if (player instanceof ServerPlayer serverPlayer) {
            Networking.sendToPlayerClient(new PacketUpdateMana(0, mana.getMaxMana(),
                    mana.getGlyphBonus(), mana.getBookTier()), serverPlayer);
        }
    }

    // ------------------------------------------------------------ 护目镜

    /**
     * 工程师护目镜信息：转速低于 {@link #MIN_RPM} 时补一行红字提示。
     * 原版 Create 的护目镜只会列出转速和应力冲击，不会说"这台机器现在开不了工"。
     * <p>
     * 注意 {@code super.addToGoggleTooltip} 只会在客户端被调用（护目镜是客户端渲染），
     * 所以判定逻辑单独抽成 {@link #stressWarning()} 便于在服务端测试里验证。
     */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean handled = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        Optional<Component> warning = stressWarning();
        warning.ifPresent(tooltip::add);
        return handled || warning.isPresent();
    }

    /**
     * 转速不足时返回要显示的那一行，够快则返回空。
     */
    Optional<Component> stressWarning() {
        if (Math.abs(getSpeed()) >= MIN_RPM) {
            return Optional.empty();
        }
        return Optional.of(Component.translatable("goggles.solaris_resources.stress_too_low", (int) MIN_RPM)
                .withStyle(ChatFormatting.RED));
    }

    // ------------------------------------------------------------ 加工

    /**
     * 一次处理需要的最低转速。低于它整台机器停转（进度保留，不会回退）。
     */
    public static final float MIN_RPM = 32.0F;
    /**
     * 时间曲线的高转速标定点；再快也不会短于 {@link #TICKS_AT_FAST_RPM}。
     */
    public static final float FAST_RPM = 256.0F;
    public static final int TICKS_AT_MIN_RPM = 200;
    public static final int TICKS_AT_FAST_RPM = 20;
    /**
     * 每次处理消耗：1B 水 + 15 魔源。
     */
    public static final int WATER_PER_PROCESS = 1000;
    public static final int SOURCE_PER_PROCESS = 15;

    private float progress;
    private int completedProcesses;

    /**
     * 转速 → 一次处理的 tick 数。32 rpm 是 200 tick、256 rpm 是 20 tick，
     * 中间按转速线性插值，两端各截断（低于 32 返回 0 = 不处理，高于 256 固定 20）。
     */
    public static int ticksPerProcess(float rpm) {
        if (rpm < MIN_RPM) {
            return 0;
        }
        float factor = Mth.clamp((rpm - MIN_RPM) / (FAST_RPM - MIN_RPM), 0.0F, 1.0F);
        return Math.round(Mth.lerp(factor, TICKS_AT_MIN_RPM, TICKS_AT_FAST_RPM));
    }

    /**
     * 当前转速下完成一次处理需要的 tick，0 表示转速不足。
     */
    public int getTicksPerProcess() {
        return ticksPerProcess(Math.abs(getSpeed()));
    }

    /**
     * 0~1 的处理进度。
     */
    public float getProgress() {
        return progress;
    }

    /**
     * 累计完成次数；产出逻辑可以读取或自行重置。
     */
    public int getCompletedProcesses() {
        return completedProcesses;
    }

    // ------------------------------------------------------------ 输出槽

    /**
     * 三个输出槽，每个只存一种物品、上限 64；任一槽满就停机。
     */
    public static final int OUTPUT_SLOTS = 3;
    public static final int OUTPUT_SLOT_LIMIT = 64;
    public static final String OUTPUT_SLOT_ID_PREFIX = "sola_output_";

    /**
     * 输出存储。{@link ItemStackTransfer} 自带 NBT 序列化，这里只收紧两条规则：
     * 每槽上限 64，且已装东西的槽只接受同类同 NBT 的物品。
     */
    private final ItemStackTransfer outputs = new ItemStackTransfer(OUTPUT_SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return OUTPUT_SLOT_LIMIT;
        }

        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            ItemStack current = getStackInSlot(slot);
            return current.isEmpty() || ItemStack.isSameItemSameTags(current, stack);
        }
    };

    public IItemTransfer getOutputs() {
        return outputs;
    }

    /**
     * 对外的物品能力：漏斗、管道、Create 装卸器都能从输出槽**抽走**，
     * 但任何外部设备都塞不进来（产物只能由机器自己写）。
     */
    private final LazyOptional<IItemHandler> outputsCapability = LazyOptional.of(() -> new IItemHandler() {
        @Override
        public int getSlots() {
            return outputs.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return outputs.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack extracted = outputs.extractItem(slot, amount, simulate, !simulate);
            if (!simulate && !extracted.isEmpty()) {
                onStateChanged();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return outputs.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    });

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER) {
            return outputsCapability.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        outputsCapability.invalidate();
    }

    /**
     * 槽位是否已满：按物品自身的堆叠上限算，所以不可堆叠的物品装满 1 个就算满。
     */
    public boolean isOutputSlotFull(int slot) {
        ItemStack stack = outputs.getStackInSlot(slot);
        return !stack.isEmpty() && stack.getCount() >= Math.min(OUTPUT_SLOT_LIMIT, stack.getMaxStackSize());
    }

    public boolean isAnyOutputFull() {
        for (int slot = 0; slot < outputs.getSlots(); slot++) {
            if (isOutputSlotFull(slot)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 产物入口：依次尝试三个槽，返回塞不下的余量。
     * 允许部分放入，适合"能塞多少塞多少"的场景。
     */
    public ItemStack insertOutput(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack remaining = stack;
        for (int slot = 0; slot < outputs.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = outputs.insertItem(slot, remaining, false, false);
        }
        if (remaining.getCount() != stack.getCount()) {
            onStateChanged();
        }
        return remaining;
    }

    /**
     * 一次调用最多放 64 个，和单个槽位的上限一致。
     */
    public static final int MAX_OUTPUT_PER_CALL = OUTPUT_SLOT_LIMIT;

    /**
     * 校验请求：物品非空、不是空气，数量在 1..64 之间。
     */
    public static boolean isValidOutputRequest(@Nullable Item item, int count) {
        return item != null && item != Items.AIR && count > 0 && count <= MAX_OUTPUT_PER_CALL;
    }

    /**
     * 三个输出槽合计还能放下多少个这种物品（按物品自身堆叠上限算）。
     */
    public int countOutputSpace(ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        int limit = Math.min(OUTPUT_SLOT_LIMIT, stack.getMaxStackSize());
        int space = 0;
        for (int slot = 0; slot < outputs.getSlots(); slot++) {
            ItemStack current = outputs.getStackInSlot(slot);
            if (current.isEmpty()) {
                space += limit;
            } else if (ItemStack.isSameItemSameTags(current, stack)) {
                space += Math.max(0, limit - current.getCount());
            }
        }
        return space;
    }

    /**
     * 产出逻辑用的入口：先校验物品种类与数量，再确认三个槽**整批**放得下，最后才写入。
     * 放不下就整批不放（不会出现"扣了 32 个只进 10 个"的静默丢失）。
     *
     * @param item  产物物品，不能是空气
     * @param count 数量，1..64
     * @return 是否整批放入
     */
    public boolean addOutput(@Nullable Item item, int count) {
        if (!isValidOutputRequest(item, count)) {
            return false;
        }
        return addOutput(new ItemStack(item, count));
    }

    /**
     * 同上，但直接收成品堆叠；数量同样限制在 1..64。
     */
    public boolean addOutput(ItemStack stack) {
        if (stack.isEmpty() || stack.getCount() > MAX_OUTPUT_PER_CALL) {
            return false;
        }
        if (countOutputSpace(stack) < stack.getCount()) {
            return false;
        }
        insertOutput(stack.copy());
        return true;
    }

    /**
     * 某个输出槽里的物品（只读副本，别拿去改）。
     */
    public ItemStack getOutput(int slot) {
        return outputs.getStackInSlot(slot);
    }

    /**
     * 推进一次处理。抽成独立方法，一是让 tick 只管调用，二是 GameTest 可以直接喂转速验证曲线与消耗。
     *
     * @return 本次调用是否刚好完成了一次处理
     */
    public boolean tickProcessing(float rpm) {
        int ticks = ticksPerProcess(rpm);
        if (ticks <= 0) {
            return false;
        }
        if (!hasProcessInputs()) {
            return false;
        }
        progress += 1.0F / ticks;
        if (progress < 1.0F) {
            setChanged();
            return false;
        }
        progress = 0.0F;
        return completeProcess();
    }

    private void tickProcessing() {
        tickProcessing(Math.abs(getSpeed()));
    }

    /**
     * 水（类型必须是水）、魔源都得够，且三个输出槽不能有任何一个满，否则停机等料/等空位。
     */
    public boolean hasProcessInputs() {
        return source >= SOURCE_PER_PROCESS && !isAnyOutputFull() && canDrainWater();
    }

    private boolean canDrainWater() {
        IFluidHandler tank = waterTank();
        if (tank == null) {
            return false;
        }
        FluidStack simulated = tank.drain(new FluidStack(Fluids.WATER, WATER_PER_PROCESS), IFluidHandler.FluidAction.SIMULATE);
        return simulated.getAmount() >= WATER_PER_PROCESS && simulated.getFluid().is(FluidTags.WATER);
    }

    /**
     * 正下方的铜流体储罐（和界面上流体槽是同一个）。
     */
    private IFluidHandler waterTank() {
        Level level = getLevel();
        if (level == null || !MagicGenMachine.hasFluidTankBelow(level, worldPosition)) {
            return null;
        }
        BlockEntity tankEntity = level.getBlockEntity(worldPosition.below());
        if (tankEntity == null) {
            return null;
        }
        return tankEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, Direction.UP).orElse(null);
    }

    private boolean completeProcess() {
        IFluidHandler tank = waterTank();
        if (tank == null || source < SOURCE_PER_PROCESS) {
            return false;
        }
        FluidStack drained = tank.drain(new FluidStack(Fluids.WATER, WATER_PER_PROCESS), IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() < WATER_PER_PROCESS || !drained.getFluid().is(FluidTags.WATER)) {
            // 并发/类型不符时把已经抽出来的还回去，别把水吃掉。
            if (!drained.isEmpty()) {
                tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            }
            return false;
        }
        removeSource(SOURCE_PER_PROCESS);
        completedProcesses++;
        onProcessComplete();
        onStateChanged();
        return true;
    }

    /**
     * 一次处理完成时的产出钩子：三条产出规则来自
     * {@code config/solaris_resources/machine/magic_core_I.json}。
     * <p>
     * 语义和原先的 if / else if 一致：按顺序找第一条命中的规则，命中就只产出这一条；
     * 物品留空的规则永远不命中（等于该槽还没配置产出）。
     */
    protected void onProcessComplete() {
        int roll = SolarisResources.RND.nextInt(100);
        for (MachineConfigStore.OutputSpec output : MachineConfigs.MAGIC_CORE.outputs()) {
            if (output.isDisabled() || !compare(roll, output.chanceMin(), output.chanceMax())) {
                continue;
            }
            addOutput(output.item(), SolarisResources.RND.nextInt(output.countMin(), output.countMax() + 1));
            return;
        }
    }

    private boolean compare(int inte, int min, int max) {
        return inte >= min && inte < max;
    }

    private void onStateChanged() {
        setChanged();
        if (level != null && !level.isClientSide) {
            sendData();
        }
    }

    /** 配置里写的是物品 ID，这里查注册表再交给带校验的 {@link #addOutput(Item, int)}。 */
    private void addOutput(@Nullable String id, int count) {
        if (id == null || id.isBlank()) {
            return;
        }
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location == null) {
            return;
        }
        Item item = ForgeRegistries.ITEMS.getValue(location);
        if (item == null) {
            return;
        }
        addOutput(item, count);
    }

    @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.putInt(SOURCE_TAG, source);
        tag.putInt(MAX_SOURCE_TAG, maxSource);
        tag.putBoolean(ABSORBING_TAG, absorbing);
        tag.putFloat(PROGRESS_TAG, progress);
        tag.putInt(COMPLETED_TAG, completedProcesses);
        tag.put(OUTPUTS_TAG, outputs.serializeNBT());
    }

    @Override
    protected void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        this.maxSource = tag.contains(MAX_SOURCE_TAG) ? tag.getInt(MAX_SOURCE_TAG) : DEFAULT_MAX_SOURCE;
        if (this.maxSource <= 0) {
            this.maxSource = DEFAULT_MAX_SOURCE;
        }
        this.source = Mth.clamp(tag.getInt(SOURCE_TAG), 0, this.maxSource);
        this.absorbing = tag.getBoolean(ABSORBING_TAG);
        this.progress = Mth.clamp(tag.getFloat(PROGRESS_TAG), 0.0F, 1.0F);
        this.completedProcesses = Math.max(0, tag.getInt(COMPLETED_TAG));
        if (tag.contains(OUTPUTS_TAG)) {
            outputs.deserializeNBT(tag.getCompound(OUTPUTS_TAG));
        }
    }

    // ------------------------------------------------------------ UI

    /**
     * 两端都会调用，必须只用双端都可用的数据，不能出现客户端专属分支。
     */
    @Override
    public ModularUI createUI(Player entityPlayer) {
        WidgetGroup root = SolarisResources.readUI(MAGIC_CORE_UI).get();
        bindTanks(root, getLevel(), getBlockPos());
        for (SourceSlotWidget widget : root.getWidgetsByType(SourceSlotWidget.class)) {
            widget.setSourceStorage(SourceStorages.of(this));
        }
        bindAbsorbButton(root, entityPlayer);
        bindProgress(root);
        bindOutputSlots(root);
        return new ModularUI(root, this, entityPlayer);
    }

    /**
     * 把编辑器里三个物品槽接到输出存储；槽位暂时没画进 .ui 时静默跳过。
     */
    private void bindOutputSlots(WidgetGroup root) {
        for (int slot = 0; slot < OUTPUT_SLOTS; slot++) {
            Widget widget = root.getFirstWidgetById(OUTPUT_SLOT_ID_PREFIX + slot);
            if (widget instanceof SlotWidget slotWidget) {
                slotWidget.setHandlerSlot(outputs, slot);
            }
        }
    }

    /**
     * 把编辑器里的进度条接到处理进度。{@link ProgressWidget} 自己带服务端→客户端的同步
     * （{@code detectAndSendChanges} → {@code writeUpdateInfo}），所以这里只给一个取值器。
     */
    private void bindProgress(WidgetGroup root) {
        Widget widget = root.getFirstWidgetById(PROGRESS_ID);
        if (widget instanceof ProgressWidget progress) {
            progress.setProgressSupplier(this::getProgress);
        }
    }

    /**
     * 把编辑器里那个开关（{@code SwitchWidget}）绑成"吸取开关"。
     * 点击会在客户端和（经 {@code handleClientAction}）服务端各回调一次，用
     * {@code ClickData#isRemote} 区分，只在服务端真正改状态；{@code setSupplier}
     * 让开关位置跟随机器状态（服务端 detectAndSendChanges 会把变化同步给客户端）。
     */
    private void bindAbsorbButton(WidgetGroup root, Player entityPlayer) {
        Widget widget = root.getFirstWidgetById(ABSORB_BUTTON_ID);
        if (!(widget instanceof SwitchWidget toggle)) {
            return;
        }
        toggle.setSupplier(this::isAbsorbing);
        toggle.setOnPressCallback((clickData, pressed) -> {
            if (clickData.isRemote) {
                return;
            }
            setAbsorbing(pressed);
            if (entityPlayer instanceof ServerPlayer serverPlayer) {
                serverPlayer.displayClientMessage(Component.translatable(isAbsorbing()
                        ? "message.solaris_resources.source_absorb.on"
                        : "message.solaris_resources.source_absorb.off"), true);
            }
        });
    }

    /**
     * 把 UI 里的流体槽接到正下方的铜流体储罐；没有储罐就保持空槽。
     */
    static void bindTanks(WidgetGroup root, Level level, BlockPos corePos) {
        if (level == null || !MagicGenMachine.hasFluidTankBelow(level, corePos)) {
            return;
        }
        IFluidTransfer tank = FluidTransferHelper.getFluidTransfer(level, corePos.below(), Direction.UP);
        if (tank == null) {
            return;
        }
        for (TankWidget widget : root.getWidgetsByType(TankWidget.class)) {
            widget.setFluidTank(tank, 0);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // The core is a static Bedrock model; no animation controllers are required.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animatableCache;
    }
}
