package dev.efm.solaris_progress.worldgen;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * init.nbt 实体放置前的 NBT 修正（纯数据处理，便于单元测试）。
 *
 * <p>结构模板里的挂饰/画用绝对坐标 {@code TileX/TileY/TileZ} 记录位置，
 * 换位置放置时不修正会刷 "invalid position" 错误日志，甚至在靠近原始坐标时吸附回原位。</p>
 */
public final class InitEntityPlacement {
    private InitEntityPlacement() {}

    /**
     * 就地修正实体 NBT：写入新坐标、去掉旧 UUID、把挂饰的 Tile 坐标改到放置点。
     *
     * @return 实体 id；NBT 残缺（缺失或为空 id）时返回 {@code null}，调用方应跳过并记录。
     */
    public static ResourceLocation prepareForPlacement(CompoundTag tag, Vec3 pos, BlockPos blockPos) {
        String rawId = tag.getString("id");
        ResourceLocation id = ResourceLocation.tryParse(rawId);
        // 注意：空串会被 tryParse 解析成 "minecraft:"（path 为空），这里必须显式判空，
        // 否则残缺实体还会走一遍 EntityType.create 并刷 "Skipping Entity with id"。
        if (rawId.isEmpty() || id == null || id.getPath().isEmpty()) {
            return null;
        }
        if (tag.contains("TileX") && tag.contains("TileY") && tag.contains("TileZ")) {
            tag.putInt("TileX", blockPos.getX());
            tag.putInt("TileY", blockPos.getY());
            tag.putInt("TileZ", blockPos.getZ());
        }
        ListTag posTag = new ListTag();
        posTag.add(DoubleTag.valueOf(pos.x));
        posTag.add(DoubleTag.valueOf(pos.y));
        posTag.add(DoubleTag.valueOf(pos.z));
        tag.put("Pos", posTag);
        tag.remove("UUID");
        return id;
    }
}
