package de.craftplay.quests.quest.player;

import de.craftplay.quests.quest.model.QuestId;
import java.util.LinkedHashMap;
import java.util.Map;

public record PlayerQuestProgress(
    QuestId questId,
    long acceptedAt,
    Map<String, Integer> objectiveProgress
) {

    public PlayerQuestProgress {
        if (questId == null) {
            throw new IllegalArgumentException("Quest id must not be null.");
        }
        if (acceptedAt < 1) {
            acceptedAt = System.currentTimeMillis();
        }
        objectiveProgress = objectiveProgress == null ? Map.of() : Map.copyOf(objectiveProgress);
    }

    public static PlayerQuestProgress started(QuestId questId) {
        return new PlayerQuestProgress(questId, System.currentTimeMillis(), Map.of());
    }

    public int objectiveValue(String objectiveId) {
        return objectiveProgress.getOrDefault(objectiveId, 0);
    }

    public PlayerQuestProgress withObjectiveProgress(String objectiveId, int value) {
        Map<String, Integer> updated = new LinkedHashMap<>(objectiveProgress);
        updated.put(objectiveId, Math.max(0, value));
        return new PlayerQuestProgress(questId, acceptedAt, updated);
    }

    public PlayerQuestProgress incrementObjectiveProgress(String objectiveId, int delta, int maxValue) {
        int current = objectiveValue(objectiveId);
        int next = Math.max(0, current + delta);
        if (maxValue > 0) {
            next = Math.min(next, maxValue);
        }
        return withObjectiveProgress(objectiveId, next);
    }
}
