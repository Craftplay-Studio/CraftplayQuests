package de.craftplay.quests.quest.model;

import java.util.List;
import java.util.Set;

public record Quest(
    QuestId id,
    String name,
    List<String> description,
    QuestMetadata metadata,
    Set<QuestId> requiresCompleted,
    Set<QuestId> unlocks,
    List<QuestRequirement> requirements,
    List<QuestObjective> objectives,
    List<QuestReward> rewards
) {

    public Quest {
        if (id == null) {
            throw new IllegalArgumentException("Quest id must not be null.");
        }
        name = name == null || name.isBlank() ? id.value() : name.trim();
        description = description == null ? List.of() : List.copyOf(description);
        metadata = metadata == null ? QuestMetadata.defaults() : metadata;
        requiresCompleted = requiresCompleted == null ? Set.of() : Set.copyOf(requiresCompleted);
        unlocks = unlocks == null ? Set.of() : Set.copyOf(unlocks);
        requirements = requirements == null ? List.of() : List.copyOf(requirements);
        objectives = objectives == null ? List.of() : List.copyOf(objectives);
        rewards = rewards == null ? List.of() : List.copyOf(rewards);
    }

    public boolean enabled() {
        return metadata.enabled();
    }
}
