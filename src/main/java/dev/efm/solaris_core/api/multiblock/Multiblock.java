package dev.efm.solaris_core.api.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

/**
 * 一台多方块机器的“图纸”。
 * <p>
 * 结构本身来自原版结构模板（<code>data/&lt;ns&gt;/structures/xxx.nbt</code>），本类只额外记录三件模板里没有的信息：
 * <ul>
 *     <li>模板 id —— 检测与还原都读这一份 .nbt，不存快照</li>
 *     <li>triggerOffset —— 控制器（master）在模板坐标系里的位置</li>
 *     <li>trigger 方块 —— 玩家必须右键这个方块才会尝试成型（通常是别的 mod 的方块）</li>
 * </ul>
 * 模板约定：默认朝向为北；成型时按点击面 / 玩家朝向做 4 向旋转，检测和还原使用同一个旋转角。
 * <p>
 * 用法：
 * <pre>{@code
 * public static final Multiblock COKE_OVEN = Multiblock.builder("solaris:coke_oven")
 *         .template("solaris:coke_oven")        // data/solaris/structures/coke_oven.nbt
 *         .triggerOffset(1, 0, 1)
 *         .trigger(Blocks.BLAST_FURNACE)        // 别人的方块
 *         .build();
 * }</pre>
 */
public final class Multiblock {

    private final ResourceLocation id;
    private final ResourceLocation templateId;
    private final BlockPos triggerOffset;
    private final Block trigger;

    private Multiblock(ResourceLocation id, ResourceLocation templateId, BlockPos triggerOffset, Block trigger) {
        this.id = id;
        this.templateId = templateId;
        this.triggerOffset = triggerOffset.immutable();
        this.trigger = trigger;
    }

    /** 机器 id，用于注册逻辑、存档里定位图纸。 */
    public ResourceLocation getId() {
        return id;
    }

    /** 结构模板 id，对应 {@code data/<ns>/structures/<path>.nbt}。 */
    public ResourceLocation getTemplateId() {
        return templateId;
    }

    /** 控制器（master）在模板坐标系里的位置。 */
    public BlockPos getTriggerOffset() {
        return triggerOffset;
    }

    /** 触发方块（点击它才会尝试成型；成型后这一格会变成 master）。 */
    public Block getTrigger() {
        return trigger;
    }

    /** 点击的方块是否是该机器的 trigger（便宜预检，不需要读模板）。 */
    public boolean isTrigger(BlockState state) {
        return state.is(trigger);
    }

    public static Builder builder(String id) {
        return builder(ResourceLocation.parse(id));
    }

    public static Builder builder(ResourceLocation id) {
        return new Builder(Objects.requireNonNull(id, "id"));
    }

    public static final class Builder {

        private final ResourceLocation id;
        private ResourceLocation templateId;
        private BlockPos triggerOffset = BlockPos.ZERO;
        private Block trigger;

        private Builder(ResourceLocation id) {
            this.id = id;
        }

        public Builder template(String templateId) {
            this.templateId = ResourceLocation.parse(templateId);
            return this;
        }

        public Builder template(ResourceLocation templateId) {
            this.templateId = Objects.requireNonNull(templateId, "templateId");
            return this;
        }

        public Builder triggerOffset(BlockPos offset) {
            this.triggerOffset = Objects.requireNonNull(offset, "offset");
            return this;
        }

        public Builder triggerOffset(int x, int y, int z) {
            this.triggerOffset = new BlockPos(x, y, z);
            return this;
        }

        public Builder trigger(Block block) {
            this.trigger = Objects.requireNonNull(block, "trigger");
            return this;
        }

        public Multiblock build() {
            if (templateId == null) {
                throw new IllegalStateException("multiblock " + id + " 缺少 template");
            }
            if (trigger == null) {
                throw new IllegalStateException("multiblock " + id + " 缺少 trigger 方块");
            }
            return new Multiblock(id, templateId, triggerOffset, trigger);
        }
    }

    @Override
    public String toString() {
        return "Multiblock[" + id + " <- " + templateId + " @ " + triggerOffset.toShortString() + "]";
    }
}
