package dev.efm.solaris_compat.solarisContract;

import dev.architectury.event.EventResult;
import dev.efm.solaris_compat.Solaris_compat;
import dev.efm.solaris_compat.data.BountyPool;
import dev.efm.solaris_compat.events.BountyCache;
import dev.ftb.mods.ftblibrary.config.Tristate;
import dev.ftb.mods.ftbquests.events.CustomRewardEvent;
import dev.ftb.mods.ftbquests.net.CreateObjectResponseMessage;
import dev.ftb.mods.ftbquests.quest.*;
import dev.ftb.mods.ftbquests.quest.reward.CustomReward;
import dev.ftb.mods.ftbquests.quest.reward.ItemReward;
import dev.ftb.mods.ftbquests.quest.reward.Reward;
import dev.ftb.mods.ftbquests.quest.task.CheckmarkTask;
import dev.ftb.mods.ftbquests.quest.task.ItemTask;
import dev.ftb.mods.ftbquests.quest.task.KillTask;
import dev.ftb.mods.ftbquests.quest.task.Task;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.*;

public class SFTBQuestsAPI {
    public static void createFTB(ServerQuestFile file) {
        boolean already = file.getChapterGroups().stream()
                .filter(g -> !g.isDefaultGroup())
                .anyMatch(g -> g.getRawTitle().equals("{title.solaris.villager_bounty}") && g.getTags().contains("solaris:villager_contract"));

        if (already) return;

        long id = file.newID();
        ChapterGroup group = new ChapterGroup(id, file);
        group.onCreated();
        group.setRawTitle("{title.solaris.villager_bounty}");
        addTag(group, "solaris:villager_contract");

        long id0 = file.newID();
        Chapter chapter = new Chapter(id0, file, group, "villager_contract");
        chapter.setRawTitle("{title.solaris.contracts}");
        group.addChapter(chapter);
        addTag(chapter, "solaris:villager_contract");

        Quest quest_cant_complete = new Quest(file.newID(), chapter);

        try {
            BOOL_FIELD.set(quest_cant_complete, true);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        CheckmarkTask task = new CheckmarkTask(file.newID(), quest_cant_complete);
        quest_cant_complete.addTask(task);

        chapter.addQuest(quest_cant_complete);

        file.refreshIDMap();
        file.markDirty();

        file.saveNow();
    }

    private static final Field TAGS_FIELD;
    private static final Field RESOURCE_FIELD;
    private static final Field LONG_FIELD;
    private static final Field ITEMSTACK_FIELD;
    private static final Field COUNT_FIELD;
    private static final Field BOOL_FIELD;

    static {
        try {
            TAGS_FIELD = QuestObjectBase.class.getDeclaredField("tags");
            TAGS_FIELD.setAccessible(true);

            RESOURCE_FIELD = KillTask.class.getDeclaredField("entity");
            RESOURCE_FIELD.setAccessible(true);

            LONG_FIELD = KillTask.class.getDeclaredField("value");
            LONG_FIELD.setAccessible(true);

            ITEMSTACK_FIELD = ItemReward.class.getDeclaredField("item");
            ITEMSTACK_FIELD.setAccessible(true);

            COUNT_FIELD = ItemReward.class.getDeclaredField("count");
            COUNT_FIELD.setAccessible(true);

            BOOL_FIELD = Quest.class.getDeclaredField("invisibleUntilCompleted");
            BOOL_FIELD.setAccessible(true);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void addTag(QuestObjectBase obj, String tag) {
        try {
            ((List<String>) TAGS_FIELD.get(obj)).add(tag);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setEntity(String id, int count, KillTask task) {
        try {
            RESOURCE_FIELD.set(task, ResourceLocation.bySeparator(id, ':'));
            LONG_FIELD.set(task, (long) count);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setReward(ItemStack stack, int count, ItemReward reward) {
        try {
            ITEMSTACK_FIELD.set(reward, stack);
            COUNT_FIELD.set(reward, count);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private static Vec3 findFreePosition(Chapter chapter) {
        Set<String> occupied = new HashSet<>();
        for (QuestObjectBase obj : chapter.getQuests()) {
            if (obj instanceof Quest quest) {
                occupied.add(quest.getX() + "," + quest.getY());
            }
        }
        Vec3 returnValue = null;
        boolean check = true;
        for (int y = 0; check; y++) {
            for (int i = 0; i < 10; i++) {
                double px = i * 5d;
                double py = y * 5d;

                if (!occupied.contains(px + "," + py)) {
                    returnValue = new Vec3(px, py, 0);
                    check = false;
                    break;
                }
            }
            y++;
        }
        return returnValue;
    }

    public static void createFTBContract(ServerQuestFile file) {
        ChapterGroup group = null;
        for (int i = 0; i < file.getChapterGroups().size(); i++) {
            ChapterGroup group1 = file.getChapterGroups().get(i);
            if (group1.getTags().contains("solaris:villager_contract")) {
                group = group1;
                break;
            }
        }

        if (group != null) {
            group.getChapters().forEach(chapter -> {
                if (chapter.getTags().contains("solaris:villager_contract")) {
                    List<BountyPool> pools = randomPoolChoose();
                    if (pools != null) {
                        BountyPool pool = getRandomPool(pools);

                        Quest contract = new Quest(file.newID(), chapter);
                        contract.setRawTitle("{title.solaris." + pool.getName().toLowerCase() + "}");
                        contract.setRawSubtitle("{rarity.solaris." + pool.getRarity().name().toLowerCase() + "}");

                        if (pool.getRequireItems() != null) pool.getRequireItems().forEach(itemStack -> {
                            var task = new ItemTask(file.newID(), contract);
                            task.setStackAndCount(itemStack.getItem().getDefaultInstance(), itemStack.getCount());
                            task.setConsumeItems(Tristate.TRUE);
                            contract.addTask(task);
                        });

                        if (pool.getRequireEntities() != null) {
                            pool.getRequireEntities().forEach(entityData -> {
                                var task = new KillTask(file.newID(), contract);
                                setEntity(entityData.getEntityId(), entityData.getCount(), task);
                                contract.addTask(task);
                            });
                        }

                        Vec3 pos = findFreePosition(chapter);
                        contract.setX(pos.x);
                        contract.setY(pos.y);
                        contract.setSize(2);

                        pool.getRewards().forEach(itemStack -> {
                            ItemReward reward = new ItemReward(file.newID(), contract);
                            setReward(itemStack, itemStack.getCount(), reward);
                            CustomReward reward0 = new CustomReward(file.newID(), contract);
                            addTag(reward0, "solaris:villager_contract");
                            reward0.setRawTitle("{tip.solaris.completed_to_remove}");

                            contract.addReward(reward);
                            contract.addReward(reward0);
                        });

                        contract.onCreated();
                        file.refreshIDMap();
                        file.clearCachedData();
                        file.markDirty();
                        file.saveNow();

                        CompoundTag tag = new CompoundTag();
                        contract.writeData(tag);

                        new CreateObjectResponseMessage(contract, null).sendToAll(file.server);

                        for (Task task : contract.getTasks()) {
                            CompoundTag tag1 = new CompoundTag();
                            tag1.putString("type", task.getType().getTypeForNBT());
                            new CreateObjectResponseMessage(task, tag1).sendToAll(file.server);
                        }

                        for (Reward reward : contract.getRewards()) {
                            CompoundTag extra = new CompoundTag();
                            extra.putString("type", reward.getType().getTypeForNBT());
                            new CreateObjectResponseMessage(reward, extra).sendToAll(file.server);
                        }
                    }
                }
            });
        }
    }

    private static List<Integer> randomList = new ArrayList<>(Solaris_compat.randomListHundred);

    private static List<BountyPool> randomPoolChoose() {
        var newlist = new ArrayList<>(randomList);
        Collections.shuffle(newlist);
        List<Integer> common = newlist.subList(0, 59);
        List<Integer> uncommon = newlist.subList(60, 79);
        List<Integer> epic = newlist.subList(80, 94);
        List<Integer> rare = newlist.subList(95, 99);

        Integer integer = Solaris_compat.random.nextInt(100);

        if (common.contains(integer)) {
            return BountyCache.getCachePools().stream().map((pool) -> (BountyPool) pool).toList();
        } else if (uncommon.contains(integer)) {
            return BountyCache.getCacheUncommonPools().stream().map((pool) -> (BountyPool) pool).toList();
        } else if (epic.contains(integer)) {
            return BountyCache.getCacheEpicPools().stream().map((pool) -> (BountyPool) pool).toList();
        } else if (rare.contains(integer)) {
            return BountyCache.getCacheRarePools().stream().map((pool) -> (BountyPool) pool).toList();
        }

        return null;
    }

    private static BountyPool getRandomPool(List<BountyPool> list) {
        return list.get(Solaris_compat.random.nextInt(list.size()));
    }

    public static EventResult onRewardGot(CustomRewardEvent evt) {
        CustomReward reward = evt.getReward();
        if (reward.hasTag("solaris:villager_contract")) {
            ServerQuestFile file = (ServerQuestFile) reward.getQuestFile();

            file.server.execute(() -> {
                file.deleteObject(reward.getQuest().id);
            });
        }
        return EventResult.pass();
    }
}
