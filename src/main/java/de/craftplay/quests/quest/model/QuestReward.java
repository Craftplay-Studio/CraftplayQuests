package de.craftplay.quests.quest.model;

import java.util.Map;

public record QuestReward(
    String id,
    RewardType type,
    Map<String, String> data
) {

    public QuestReward {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Reward id must not be blank.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Reward type must not be null.");
        }
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
