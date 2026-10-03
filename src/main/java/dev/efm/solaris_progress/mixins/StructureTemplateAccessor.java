package dev.efm.solaris_progress.mixins;

import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

/** 读取结构模板的实体表，用于带修正的实体放置（原版字段为私有）。 */
@Mixin(StructureTemplate.class)
public interface StructureTemplateAccessor {
    @Accessor("entityInfoList")
    List<StructureTemplate.StructureEntityInfo> solaris$getEntityInfoList();
}
