package dev.efm.solaris_core.api.multiblock;

import dev.efm.solaris_core.api.multiblock.event.MultiblockFormEvent;
import dev.efm.solaris_core.common.SolarisRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 多方块的成型 / 拆解入口。全部只应在服务端调用。
 * <p>
 * 成型：读 {@code .nbt} 模板 → 找一个能和世界对准的旋转（优先点击面）→ 逐格精确比对 →
 * 发 {@link MultiblockFormEvent} → 先铺 slave 再放 master。
 * <p>
 * 拆解：读同一个 {@code .nbt} + master 里存的旋转，逐格还原成材料；被敲的那一格留空。
 */
public final class MultiblockHandler {

    private static final Rotation[] ALL_ROTATIONS = {
            Rotation.NONE,
            Rotation.CLOCKWISE_90,
            Rotation.CLOCKWISE_180,
            Rotation.COUNTERCLOCKWISE_90
    };

    private MultiblockHandler() {
    }

    /**
     * 尝试用点击位置成型：遍历所有已注册图纸，先比 trigger 方块，再逐格比对模板。
     *
     * @param clickedFace 玩家点击的面，用于决定旋转（水平面优先，否则用玩家朝向）
     * @return 是否成功成型
     */
    public static boolean tryForm(ServerPlayer player, BlockPos clickedPos, @Nullable Direction clickedFace) {
        ServerLevel level = player.serverLevel();
        BlockState clicked = level.getBlockState(clickedPos);
        for (Multiblock multiblock : MultiblockRegistry.all()) {
            if (!multiblock.isTrigger(clicked)) {
                continue;
            }
            if (form(level, player, clickedPos, clickedFace, multiblock)) {
                return true;
            }
        }
        return false;
    }

    private static boolean form(ServerLevel level, ServerPlayer player, BlockPos clickedPos,
                                @Nullable Direction clickedFace, Multiblock multiblock) {
        StructureTemplate template = level.getStructureManager().get(multiblock.getTemplateId()).orElse(null);
        if (template == null) {
            return false;
        }
        List<StructureTemplate.StructureBlockInfo> blocks = blocksOf(template);
        if (blocks.isEmpty()) {
            return false;
        }

        Rotation rotation = findRotation(level, multiblock, blocks, clickedPos, preferredRotation(player, clickedFace));
        if (rotation == null) {
            return false;
        }
        if (MinecraftForge.EVENT_BUS.post(new MultiblockFormEvent(level, clickedPos, multiblock, player))) {
            return false;
        }

        BlockPos origin = origin(multiblock, clickedPos, rotation);
        BlockState masterState = SolarisRegistry.PART_BLOCK.get().defaultBlockState();
        BlockState slaveState = masterState.setValue(SolarisPartBlock.SLAVE, true);

        // 先把 slave 铺满，master 最后放（放 master 会创建 BlockEntity）
        List<BlockPos> partOffsets = new ArrayList<>();
        partOffsets.add(BlockPos.ZERO);
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            if (info.state().isAir()) {
                continue;
            }
            BlockPos abs = origin.offset(info.pos().rotate(rotation));
            if (abs.equals(clickedPos)) {
                continue;
            }
            level.setBlock(abs, slaveState, Block.UPDATE_ALL);
            if (level.getBlockEntity(abs) instanceof SolarisDummyBlockEntity dummy) {
                // masterOffset 的约定是「本格 - master」，dummy 用 worldPosition.subtract(masterOffset) 反查
                dummy.setMasterOffset(abs.subtract(clickedPos));
            }
            partOffsets.add(abs.subtract(clickedPos));
        }
        level.setBlock(clickedPos, masterState, Block.UPDATE_ALL);
        if (level.getBlockEntity(clickedPos) instanceof SolarisMasterBlockEntity master) {
            master.setup(multiblock.getId(), rotation, template.getSize(), partOffsets);
            IMultiblockLogic logic = MultiblockLogics.get(multiblock.getId());
            if (logic != null) {
                logic.onFormed(level, master);
            }
        }
        return true;
    }

    /**
     * 拆解：把整座结构按模板还原成材料。
     *
     * @param brokenPos 被破坏的那一格（{@code null} 表示全部还原；非 null 时该格留空）
     */
    public static void disassemble(ServerLevel level, SolarisMasterBlockEntity master, @Nullable BlockPos brokenPos) {
        if (master.isDisassembling()) {
            return;
        }
        master.setDisassembling(true);
        try {
            ResourceLocation multiblockId = master.getMultiblockId();
            Multiblock multiblock = multiblockId == null ? null : MultiblockRegistry.get(multiblockId);
            if (multiblock == null) {
                return;
            }
            StructureTemplate template = level.getStructureManager().get(multiblock.getTemplateId()).orElse(null);
            if (template == null) {
                return;
            }
            List<StructureTemplate.StructureBlockInfo> blocks = blocksOf(template);
            BlockPos masterPos = master.getBlockPos();
            BlockPos origin = origin(multiblock, masterPos, master.getRotation());

            IMultiblockLogic logic = MultiblockLogics.get(multiblockId);
            if (logic != null) {
                logic.onBroken(level, master);
            }

            // 其它格：只覆盖还是 part 方块的位置，避免把玩家后来放的东西吃掉
            for (StructureTemplate.StructureBlockInfo info : blocks) {
                if (info.state().isAir()) {
                    continue;
                }
                BlockPos abs = origin.offset(info.pos().rotate(master.getRotation()));
                if (abs.equals(brokenPos) || abs.equals(masterPos)) {
                    continue;
                }
                if (!(level.getBlockState(abs).getBlock() instanceof SolarisPartBlock)) {
                    continue;
                }
                level.setBlock(abs, info.state().rotate(master.getRotation()), Block.UPDATE_ALL);
            }

            // master 最后还原（会移除它的 BlockEntity）
            if (!masterPos.equals(brokenPos)) {
                BlockState original = triggerState(blocks, multiblock, master.getRotation());
                if (original != null) {
                    level.setBlock(masterPos, original, Block.UPDATE_ALL);
                }
            }
        } finally {
            master.setDisassembling(false);
        }
    }

    /** 优先用点击面，其次玩家朝向；点击面是水平面时按它旋转。 */
    public static Rotation preferredRotation(Player player, @Nullable Direction clickedFace) {
        if (clickedFace != null && clickedFace.getAxis().isHorizontal()) {
            return rotationOf(clickedFace);
        }
        return rotationOf(player.getDirection());
    }

    private static Rotation rotationOf(Direction direction) {
        return switch (direction) {
            case EAST -> Rotation.CLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    @Nullable
    private static Rotation findRotation(ServerLevel level, Multiblock multiblock,
                                         List<StructureTemplate.StructureBlockInfo> blocks,
                                         BlockPos clickedPos, Rotation preferred) {
        if (matches(level, multiblock, blocks, clickedPos, preferred)) {
            return preferred;
        }
        for (Rotation rotation : ALL_ROTATIONS) {
            if (rotation != preferred && matches(level, multiblock, blocks, clickedPos, rotation)) {
                return rotation;
            }
        }
        return null;
    }

    private static boolean matches(ServerLevel level, Multiblock multiblock,
                                   List<StructureTemplate.StructureBlockInfo> blocks,
                                   BlockPos clickedPos, Rotation rotation) {
        BlockPos origin = origin(multiblock, clickedPos, rotation);
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            if (info.state().isAir()) {
                continue;
            }
            BlockPos abs = origin.offset(info.pos().rotate(rotation));
            if (!level.isLoaded(abs)) {
                return false;
            }
            if (!level.getBlockState(abs).equals(info.state().rotate(rotation))) {
                return false;
            }
        }
        return true;
    }

    private static BlockPos origin(Multiblock multiblock, BlockPos masterPos, Rotation rotation) {
        return masterPos.subtract(multiblock.getTriggerOffset().rotate(rotation));
    }

    private static List<StructureTemplate.StructureBlockInfo> blocksOf(StructureTemplate template) {
        return template.palettes.isEmpty() ? List.of() : template.palettes.get(0).blocks();
    }

    @Nullable
    private static BlockState triggerState(List<StructureTemplate.StructureBlockInfo> blocks,
                                           Multiblock multiblock, Rotation rotation) {
        for (StructureTemplate.StructureBlockInfo info : blocks) {
            if (info.pos().equals(multiblock.getTriggerOffset())) {
                return info.state().rotate(rotation);
            }
        }
        return null;
    }
}
