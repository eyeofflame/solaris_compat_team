package dev.efm.solaris_progress;

import dev.ftb.mods.ftbquests.quest.QuestObjectBase;
import dev.ftb.mods.ftbquests.quest.ServerQuestFile;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.Task;
import dev.ftb.mods.ftbquests.util.ProgressChange;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class SolaAPI {
    public static void completeTaskForTeam(ServerPlayer anyMember, String taskIdHex) {
        long id = QuestObjectBase.parseCodeString(taskIdHex);

        Task task = ServerQuestFile.INSTANCE.getTask(id);

        TeamData data = ServerQuestFile.INSTANCE.getOrCreateTeamData(anyMember);

        ProgressChange change = new ProgressChange(ServerQuestFile.INSTANCE, task, anyMember.getUUID()).setReset(false);
        if (task != null) {
            task.forceProgress(data, change);
        }
    }

    public static ServerLevel getLevel(String id) {
        var rid = ResourceLocation.tryParse(id);
        if (rid != null) {
            return ServerLifecycleHooks.getCurrentServer().getLevel(ResourceKey.create(Registries.DIMENSION, rid));
        }
        return null;
    }
}
