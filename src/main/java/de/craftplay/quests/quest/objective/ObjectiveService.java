package de.craftplay.quests.quest.objective;

import de.craftplay.quests.quest.model.ObjectiveType;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.model.QuestObjective;
import de.craftplay.quests.quest.player.PlayerQuestData;
import de.craftplay.quests.quest.player.PlayerQuestProgress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ObjectiveService {

    public boolean isQuestComplete(Quest quest, PlayerQuestData data) {
        Optional<PlayerQuestProgress> progress = data.progress(quest.id());
        if (progress.isEmpty()) {
            return false;
        }
        return isQuestComplete(quest, progress.get());
    }

    public boolean isQuestComplete(Quest quest, PlayerQuestProgress progress) {
        if (quest.objectives().isEmpty()) {
            return true;
        }
        return quest.objectives().stream().allMatch(objective -> isObjectiveComplete(objective, progress));
    }

    public boolean isObjectiveComplete(QuestObjective objective, PlayerQuestProgress progress) {
        return progress.objectiveValue(objective.id()) >= objective.amount();
    }

    public ObjectiveProgressResult applyProgress(
        PlayerQuestData data,
        Collection<Quest> activeQuests,
        ObjectiveType type,
        String target,
        int amount
    ) {
        if (amount <= 0) {
            return new ObjectiveProgressResult(data, List.of(), List.of());
        }

        PlayerQuestData updatedData = data;
        ArrayList<QuestId> changed = new ArrayList<>();
        ArrayList<QuestId> completed = new ArrayList<>();

        for (Quest quest : activeQuests) {
            Optional<PlayerQuestProgress> currentProgress = updatedData.progress(quest.id());
            if (currentProgress.isEmpty()) {
                continue;
            }

            PlayerQuestProgress progress = currentProgress.get();
            PlayerQuestProgress updatedProgress = progress;
            boolean questChanged = false;

            for (QuestObjective objective : quest.objectives()) {
                if (!matches(objective, type, target) || isObjectiveComplete(objective, updatedProgress)) {
                    continue;
                }
                updatedProgress = updatedProgress.incrementObjectiveProgress(objective.id(), amount, objective.amount());
                questChanged = true;
            }

            if (!questChanged) {
                continue;
            }

            updatedData = updatedData.withProgress(updatedProgress);
            changed.add(quest.id());

            if (isQuestComplete(quest, updatedProgress)) {
                completed.add(quest.id());
            }
        }

        return new ObjectiveProgressResult(updatedData, changed, completed);
    }

    public int currentAmount(QuestObjective objective, PlayerQuestProgress progress) {
        return progress.objectiveValue(objective.id());
    }

    private boolean matches(QuestObjective objective, ObjectiveType type, String target) {
        if (objective.type() != type) {
            return false;
        }

        String objectiveTarget = normalize(objective.target());
        if (objectiveTarget.isBlank() || "*".equals(objectiveTarget)) {
            return true;
        }
        return objectiveTarget.equals(normalize(target));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
    }
}
