package de.craftplay.quests.quest.model;

import java.util.Map;

public record QuestRequirement(
    String id,
    RequirementType type,
    String target,
    String value,
    Map<String, String> data
) {

    public QuestRequirement {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Requirement id must not be blank.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Requirement type must not be null.");
        }
        target = target == null ? "" : target.trim();
        value = value == null ? "" : value.trim();
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
