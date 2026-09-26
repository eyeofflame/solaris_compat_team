package dev.efm.mekanism_agriculture.common.registration;

import com.blakebr0.cucumber.crafting.ISpecialRecipe;
import com.blakebr0.mysticalagriculture.api.crafting.IInfusionRecipe;
import com.blakebr0.mysticalagriculture.init.ModRecipeTypes;
import dev.efm.mekanism_agriculture.common.inventory.NineSlotItemHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.NBTConstants;
import mekanism.api.RelativeSide;
import mekanism.api.Upgrade;
import mekanism.api.inventory.IInventorySlot;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.inventory.slot.OutputInventorySlot;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.tile.component.TileComponentConfig;
import mekanism.common.tile.component.TileComponentEjector;
import mekanism.common.tile.component.config.ConfigInfo;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.tile.component.config.slot.InventorySlotInfo;
import mekanism.common.tile.prefab.TileEntityConfigurableMachine;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 用电的注魔机器:直接复用神秘农业的注魔(infusion)配方,等价于一台可以接电、能自动化的注魔祭坛。
 *
 * <p>为什么不是 {@code TileEntityElectricMachine}:那条继承链绑死在 {@code MekanismRecipe} 上,而
 * 附魔 mod 无法创建自己的 Mekanism 配方类型({@code MekanismRecipeType} 构造函数与 {@code register}
 * 均为 private,且 {@code IMekanismRecipeTypeProvider#getRecipeType()} 返回的是具体类而非接口)。
 * 它只能配 CRUSHING/ENRICHING/SMELTING 三种"1 物品进 1 物品出"的配方,而注魔是 <b>9 进 1 出</b>,
 * 结构上不兼容,所以这里直接继承 {@code TileEntityConfigurableMachine} 自行处理配方。
 *
 * <h2>槽位与匹配语义</h2>
 * <ul>
 *   <li><b>槽 0 = 输入1(中央核心)</b>:配方 JSON 的 {@code input} 字段,位置固定,只接受能作为某条
 *       注魔配方核心的物品。</li>
 *   <li><b>槽 1..8 = 输入2(八份辅料)</b>:对应配方 JSON 的 {@code ingredients} 数组。
 *       <b>不按摆放位置</b>,而是<b>按数量</b>匹配 —— 一个槽里堆了 4 个红石就顶 4 份,不需要一格一个。
 *       这样才能自动化:两条输入线送进来的红石和精华各占一格也能合成。</li>
 * </ul>
 * 正因如此,这里没有用神秘农业自己的 {@code ISpecialRecipe#matches(IItemHandler)} —— 那个走 Forge 的
 * {@code RecipeMatcher.findMatches},是"一个堆叠对一个 ingredient"的一对一匹配,同种物品必须分散在
 * 不同槽位里。我们要的是按数量的多对多匹配,所以匹配与消耗都自己实现。
 */
public class TileEntityMekInfusioner extends TileEntityConfigurableMachine {

    /** 与神秘农业注魔祭坛保持一致:100 tick 完成一次注魔。 */
    private static final int BASE_TICKS = 100;
    /** 配方所需的输入格数,等于 {@code InfusionRecipe.RECIPE_SIZE}。 */
    private static final int INFUSION_SLOTS = 9;
    /** 槽 1..8 的 GUI 坐标,环绕居中的槽 0,镜像注魔祭坛的八座基座。 */
    private static final int[][] RING_POSITIONS = {
            {44, 17}, {62, 17}, {80, 17}, {80, 35}, {80, 53}, {62, 53}, {44, 53}, {44, 35}
    };

    // 以下槽位字段由 getInitialInventory 赋值,而它是在【父类构造器】里被调用的。
    // 因此绝不能写成字段初始化 —— 那样会在 super() 返回之后把它们重新覆盖成 null。
    private List<IInventorySlot> infusionSlots;
    private List<IInventorySlot> reagentSlots;
    private NineSlotItemHandler infusionHandler;
    private OutputInventorySlot outputSlot;
    private EnergyInventorySlot energySlot;

    private MachineEnergyContainer<TileEntityMekInfusioner> energyContainer;

    private int operatingTicks;
    private int ticksRequired = BASE_TICKS;
    /** 当前锁定的配方。仅当它不再匹配时才重新扫描,避免每 tick 遍历全部注魔配方。 */
    @Nullable
    private IInfusionRecipe activeRecipe;
    /** 上次扫描配方表时的输入指纹;输入没变就不必重扫。 */
    private int lastScanHash;
    private boolean hasScanned;

    public TileEntityMekInfusioner(BlockPos pos, BlockState state) {
        super(MekBlocks.MEK_INFUSIONER, pos, state);
        // 槽位与能量容器都已在 super() 里建好,此处才能安全接线
        configComponent = new TileComponentConfig(this, TransmissionType.ITEM, TransmissionType.ENERGY);

        ConfigInfo itemConfig = configComponent.getConfig(TransmissionType.ITEM);
        if (itemConfig != null) {
            itemConfig.addSlotInfo(DataType.INPUT_1, new InventorySlotInfo(true, true, infusionSlots.get(0)));
            itemConfig.addSlotInfo(DataType.INPUT_2, new InventorySlotInfo(true, true, reagentSlots));
            itemConfig.addSlotInfo(DataType.OUTPUT, new InventorySlotInfo(true, true, outputSlot));
            itemConfig.addSlotInfo(DataType.INPUT_OUTPUT, new InventorySlotInfo(true, true, infusionSlots));
            itemConfig.addSlotInfo(DataType.ENERGY, new InventorySlotInfo(true, true, energySlot));
            // 默认朝向:正面收核心物,左右下收辅料,背面出料,顶面接电
            itemConfig.setDataType(DataType.INPUT_1, RelativeSide.FRONT);
            itemConfig.setDataType(DataType.INPUT_2, RelativeSide.LEFT, RelativeSide.RIGHT, RelativeSide.BOTTOM);
            itemConfig.setDataType(DataType.OUTPUT, RelativeSide.BACK);
            itemConfig.setDataType(DataType.ENERGY, RelativeSide.TOP);
        }
        configComponent.setupInputConfig(TransmissionType.ENERGY, energyContainer);

        ejectorComponent = new TileComponentEjector(this);
        ejectorComponent.setOutputData(configComponent, TransmissionType.ITEM);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSideWithConfig(this::getDirection, this::getConfig);
        // 能耗与容量直接取自方块类型上的 AttributeEnergy(500 FE/t、200k FE),并自动响应 ENERGY 升级
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener));
        return builder.build();
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSideWithConfig(this::getDirection, this::getConfig);
        infusionSlots = new ArrayList<>(INFUSION_SLOTS);
        // 槽 0 = 配方核心物(input),居中。过滤器两处都传,否则自动化仍能把任意物品塞进来
        infusionSlots.add(builder.addSlot(
                InputInventorySlot.at(this::isValidInfusionCore, this::isValidInfusionCore, listener, 62, 35)));
        // 槽 1..8 = 辅料,环绕且彼此无序、按数量匹配
        for (int[] pos : RING_POSITIONS) {
            infusionSlots.add(builder.addSlot(InputInventorySlot.at(listener, pos[0], pos[1])));
        }
        reagentSlots = List.copyOf(infusionSlots.subList(1, INFUSION_SLOTS));
        infusionHandler = new NineSlotItemHandler(infusionSlots);
        builder.addSlot(outputSlot = OutputInventorySlot.at(listener, 134, 35));
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 134, 53));
        return builder.build();
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        energySlot.fillContainerOrConvert();

        IInfusionRecipe recipe = getActiveRecipe();
        if (recipe == null) {
            operatingTicks = 0;
            setActive(false);
            return;
        }

        FloatingLong energyPerTick = energyContainer.getEnergyPerTick();
        if (!energyContainer.getEnergy().greaterOrEqual(energyPerTick)) {
            // 能量不足:进度保留,来电后接着跑
            setActive(false);
            return;
        }
        energyContainer.extract(energyPerTick, Action.EXECUTE, AutomationType.INTERNAL);
        operatingTicks++;
        setActive(true);

        if (operatingTicks >= ticksRequired) {
            finishInfusion(recipe);
            operatingTicks = 0;
            activeRecipe = null;
            setActive(false);
        }
    }

    /**
     * 取当前可执行的配方。
     *
     * <p>注魔配方数量不小,而 {@code getAllRecipesFor} 每次都会 {@code List.copyOf} 复制整张表,
     * 所以这里不能每 tick 全扫。两层缓存:已锁定的配方只要还匹配就直接沿用;只有当它不再匹配、
     * 且 9 个槽的内容相对上次扫描确实变了时,才真正重新扫描。
     *
     * <p>注意"产物放不放得下"必须每 tick 重新判断,不能并进缓存 —— 否则输出槽被下游腾空之后,
     * 机器会因为没有重新扫描而永远不再启动。
     */
    @Nullable
    private IInfusionRecipe getActiveRecipe() {
        if (level == null) {
            return null;
        }
        IInfusionRecipe recipe = activeRecipe;
        if (recipe == null || !matches(recipe)) {
            int hash = inputHash();
            if (hasScanned && hash == lastScanHash) {
                return null;
            }
            hasScanned = true;
            lastScanHash = hash;
            activeRecipe = recipe = findMatchingRecipe();
        }
        return recipe != null && canStoreResult(recipe) ? recipe : null;
    }

    /** 9 个槽的内容指纹,用来判断是否值得重新扫描配方表。 */
    private int inputHash() {
        int hash = 1;
        for (int slot = 0; slot < INFUSION_SLOTS; slot++) {
            hash = 31 * hash + infusionSlots.get(slot).getStack().hashCode();
        }
        return hash;
    }

    @Nullable
    private IInfusionRecipe findMatchingRecipe() {
        if (level == null) {
            return null;
        }
        for (IInfusionRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.INFUSION.get())) {
            if (matches(recipe)) {
                return recipe;
            }
        }
        return null;
    }

    /**
     * 中央槽必须命中 {@code ingredients[0]}(位置固定);八份辅料按数量匹配。
     */
    private boolean matches(IInfusionRecipe recipe) {
        List<Ingredient> ingredients = recipe.getIngredients();
        if (ingredients.isEmpty() || !ingredients.get(0).test(infusionSlots.get(0).getStack())) {
            return false;
        }
        return assignReagents(ingredients, 1, newReagentCounts(), new int[ingredients.size()], -1);
    }

    /** 采集八个辅料槽当前的可用数量,供按数量匹配使用。 */
    private int[] newReagentCounts() {
        int[] remaining = new int[INFUSION_SLOTS];
        for (int slot = 1; slot < INFUSION_SLOTS; slot++) {
            remaining[slot] = infusionSlots.get(slot).getStack().getCount();
        }
        return remaining;
    }

    /**
     * 把 {@code ingredients[from..]} 逐个分配到 1..8 号槽,返回是否可行。
     *
     * <p>关键点:{@code remaining[slot]} 让同一个槽可以贡献多个物品(堆叠数量),所以"一格塞一组红石"
     * 就能满足配方里多个相同的 ingredient。{@code assignment[i]} 记录第 i 个 ingredient 由哪个槽提供,
     * 便于匹配成功后按同样的分配去消耗。
     */
    private boolean assignReagents(List<Ingredient> ingredients, int from, int[] remaining, int[] assignment, int lastSlot) {
        if (from >= ingredients.size()) {
            return true;
        }
        Ingredient ingredient = ingredients.get(from);
        if (ingredient.isEmpty()) {
            // 配方没用到的槽位(EMPTY)跳过,不参与匹配
            return assignReagents(ingredients, from + 1, remaining, assignment, lastSlot);
        }
        for (int slot = 1; slot < INFUSION_SLOTS; slot++) {
            if (remaining[slot] <= 0 || !ingredient.test(infusionSlots.get(slot).getStack())) {
                continue;
            }
            // 剪枝:本槽与上一个已分配槽装的物品完全相同的话,只允许向后取,
            // 否则等价的槽位排列会被反复枚举(8 个相同物品时是 8! 条无用分支)。
            if (lastSlot >= 0 && slot < lastSlot && ItemStack.isSameItemSameTags(
                    infusionSlots.get(slot).getStack(), infusionSlots.get(lastSlot).getStack())) {
                continue;
            }
            remaining[slot]--;
            assignment[from] = slot;
            if (assignReagents(ingredients, from + 1, remaining, assignment, slot)) {
                return true;
            }
            remaining[slot]++;
            assignment[from] = -1;
        }
        return false;
    }

    /** 中央槽只接受"能作为某条注魔配方 input"的物品,避免自动化把辅料塞进来。 */
    private boolean isValidInfusionCore(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        if (level == null) {
            // 方块实体还没进世界时放宽,否则槽位机制在初始化阶段会被卡住
            return true;
        }
        for (IInfusionRecipe recipe : level.getRecipeManager().getAllRecipesFor(ModRecipeTypes.INFUSION.get())) {
            List<Ingredient> ingredients = recipe.getIngredients();
            if (!ingredients.isEmpty() && ingredients.get(0).test(stack)) {
                return true;
            }
        }
        return false;
    }

    /** 产物必须能完整放进输出槽,否则不启动,避免进度跑满却卡住。 */
    private boolean canStoreResult(IInfusionRecipe recipe) {
        if (level == null) {
            return false;
        }
        ItemStack result = recipe.getResultItem(level.registryAccess());
        return !result.isEmpty() && outputSlot.insertItem(result, Action.SIMULATE, AutomationType.INTERNAL).isEmpty();
    }

    private void finishInfusion(IInfusionRecipe recipe) {
        if (level == null) {
            return;
        }
        // 先算产物:assemble 只读中央槽(transfer_nbt 会拷它的 NBT),必须在消耗之前调用
        ItemStack result = recipe instanceof ISpecialRecipe special
                ? special.assemble(infusionHandler, level.registryAccess())
                : recipe.getResultItem(level.registryAccess());

        consumeOne(infusionSlots.get(0));

        // 用与匹配阶段完全相同的分配方式去消耗,保证"匹到什么就扣什么"
        List<Ingredient> ingredients = recipe.getIngredients();
        int[] assignment = new int[ingredients.size()];
        Arrays.fill(assignment, -1);
        if (assignReagents(ingredients, 1, newReagentCounts(), assignment, -1)) {
            for (int i = 1; i < ingredients.size(); i++) {
                if (assignment[i] >= 0) {
                    consumeOne(infusionSlots.get(assignment[i]));
                }
            }
        }

        if (!result.isEmpty()) {
            outputSlot.insertItem(result, Action.EXECUTE, AutomationType.INTERNAL);
        }
        markForSave();
    }

    /**
     * 从槽里扣掉一个物品。若扣空且该物品有容器剩余物(水桶→空桶),把剩余物留在槽里,
     * 这样带桶的配方不会被吞掉容器。
     */
    private void consumeOne(IInventorySlot slot) {
        ItemStack stack = slot.getStack().copy();
        if (stack.isEmpty()) {
            return;
        }
        ItemStack containerItem = stack.getCraftingRemainingItem();
        stack.shrink(1);
        if (stack.isEmpty() && !containerItem.isEmpty()) {
            slot.setStack(containerItem);
        } else {
            slot.setStack(stack);
        }
    }

    public MachineEnergyContainer<TileEntityMekInfusioner> getEnergyContainer() {
        return energyContainer;
    }

    public double getScaledProgress() {
        return ticksRequired == 0 ? 0 : operatingTicks / (double) ticksRequired;
    }

    public int getOperatingTicks() {
        return operatingTicks;
    }

    private void setOperatingTicks(int ticks) {
        operatingTicks = ticks;
    }

    public int getTicksRequired() {
        return ticksRequired;
    }

    @Override
    public void recalculateUpgrades(Upgrade upgrade) {
        super.recalculateUpgrades(upgrade);
        if (upgrade == Upgrade.SPEED) {
            ticksRequired = MekanismUtils.getTicks(this, BASE_TICKS);
        }
    }

    @Override
    public void load(@NotNull CompoundTag nbt) {
        super.load(nbt);
        operatingTicks = nbt.getInt(NBTConstants.PROGRESS);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbtTags) {
        super.saveAdditional(nbtTags);
        nbtTags.putInt(NBTConstants.PROGRESS, operatingTicks);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(this::getOperatingTicks, this::setOperatingTicks));
        container.track(SyncableInt.create(this::getTicksRequired, value -> ticksRequired = value));
    }
}
