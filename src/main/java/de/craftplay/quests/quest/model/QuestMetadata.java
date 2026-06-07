package de.craftplay.quests.quest.model;

import java.util.Optional;

public record QuestMetadata(
    String category,
    QuestType type,
    QuestDifficulty difficulty,
    int recommendedMinPlayers,
    int recommendedMaxPlayers,
    String estimatedDuration,
    Optional<String> npcId,
    boolean enabled
) {

    public QuestMetadata {
        category = category == null || category.isBlank() ? "default" : category.trim();
        type = type == null ? QuestType.NORMAL : type;
        difficulty = difficulty == null ? QuestDifficulty.NORMAL : difficulty;
        if (recommendedMinPlayers < 1) {
            recommendedMinPlayers = 1;
        }
        if (recommendedMaxPlayers < recommendedMinPlayers) {
            recommendedMaxPlayers = recommendedMinPlayers;
        }
        estimatedDuration = estimatedDuration == null ? "" : estimatedDuration.trim();
        npcId = npcId == null ? Optional.empty() : npcId.map(String::trim).filter(value -> !value.isBlank());
    }

    public static QuestMetadata defaults() {
        return new QuestMetadata(
            "default",
            QuestType.NORMAL,
            QuestDifficulty.NORMAL,
            1,
            1,
            "",
            Optional.empty(),
            true
        );
    }
}
