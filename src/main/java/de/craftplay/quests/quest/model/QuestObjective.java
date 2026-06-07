package de.craftplay.quests.quest.model;

import java.util.Map;

public record QuestObjective(
    String id,
    ObjectiveType type,
    String target,
    int amount,
    Map<String, String> data
) {

    public QuestObjective {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Objective id must not be blank.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Objective type must not be null.");
        }
        target = target == null ? "" : target.trim();
        if (amount < 1) {
            throw new IllegalArgumentException("Objective amount must be greater than 0.");
        }
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
