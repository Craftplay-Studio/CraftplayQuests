package de.craftplay.quests.quest.player;

import de.craftplay.quests.quest.model.QuestId;
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
}
