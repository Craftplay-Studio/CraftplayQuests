package de.craftplay.quests.quest.storage;

import de.craftplay.quests.quest.model.ObjectiveType;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestDifficulty;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.model.QuestMetadata;
import de.craftplay.quests.quest.model.QuestObjective;
import de.craftplay.quests.quest.model.QuestRequirement;
import de.craftplay.quests.quest.model.QuestReward;
import de.craftplay.quests.quest.model.QuestType;
import de.craftplay.quests.quest.model.RequirementType;
import de.craftplay.quests.quest.model.RewardType;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.bukkit.configuration.file.YamlConfiguration;

public final class QuestSerializer {

    public String serialize(Quest quest) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("id", quest.id().value());
        configuration.set("name", quest.name());
        configuration.set("description", quest.description());
        configuration.set("category", quest.metadata().category());
        configuration.set("type", quest.metadata().type().name());
        configuration.set("difficulty", quest.metadata().difficulty().name());
        configuration.set("recommended-players.min", quest.metadata().recommendedMinPlayers());
        configuration.set("recommended-players.max", quest.metadata().recommendedMaxPlayers());
        configuration.set("estimated-duration", quest.metadata().estimatedDuration());
        configuration.set("enabled", quest.metadata().enabled());
        quest.metadata().npcId().ifPresent(npcId -> configuration.set("npc-id", npcId));
        configuration.set("requires-completed", quest.requiresCompleted().stream().map(QuestId::value).sorted().toList());
        configuration.set("unlocks", quest.unlocks().stream().map(QuestId::value).sorted().toList());
        configuration.set("requirements", quest.requirements().stream().map(this::serializeRequirement).toList());
        configuration.set("objectives", quest.objectives().stream().map(this::serializeObjective).toList());
        configuration.set("rewards", quest.rewards().stream().map(this::serializeReward).toList());
        return configuration.saveToString();
    }

    public Quest deserialize(String content) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(content);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid quest YAML", exception);
        }

        QuestId id = QuestId.of(configuration.getString("id", ""));
        QuestMetadata metadata = new QuestMetadata(
            configuration.getString("category", "default"),
            enumValue(QuestType.class, configuration.getString("type", "NORMAL"), QuestType.NORMAL),
            enumValue(QuestDifficulty.class, configuration.getString("difficulty", "NORMAL"), QuestDifficulty.NORMAL),
            configuration.getInt("recommended-players.min", 1),
            configuration.getInt("recommended-players.max", 1),
            configuration.getString("estimated-duration", ""),
            Optional.ofNullable(configuration.getString("npc-id")),
            configuration.getBoolean("enabled", true)
        );

        return new Quest(
            id,
            configuration.getString("name", id.value()),
            configuration.getStringList("description"),
            metadata,
            questIdSet(configuration.getStringList("requires-completed")),
            questIdSet(configuration.getStringList("unlocks")),
            configuration.getMapList("requirements").stream().map(this::deserializeRequirement).toList(),
            configuration.getMapList("objectives").stream().map(this::deserializeObjective).toList(),
            configuration.getMapList("rewards").stream().map(this::deserializeReward).toList()
        );
    }

    private Map<String, Object> serializeRequirement(QuestRequirement requirement) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", requirement.id());
        values.put("type", requirement.type().name());
        values.put("target", requirement.target());
        values.put("value", requirement.value());
        values.put("data", requirement.data());
        return values;
    }

    private Map<String, Object> serializeObjective(QuestObjective objective) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", objective.id());
        values.put("type", objective.type().name());
        values.put("target", objective.target());
        values.put("amount", objective.amount());
        values.put("data", objective.data());
        return values;
    }

    private Map<String, Object> serializeReward(QuestReward reward) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("id", reward.id());
        values.put("type", reward.type().name());
        values.put("data", reward.data());
        return values;
    }

    private QuestRequirement deserializeRequirement(Map<?, ?> values) {
        return new QuestRequirement(
            string(values, "id", "requirement"),
            enumValue(RequirementType.class, string(values, "type", "PERMISSION"), RequirementType.PERMISSION),
            string(values, "target", ""),
            string(values, "value", ""),
            stringMap(values.get("data"))
        );
    }

    private QuestObjective deserializeObjective(Map<?, ?> values) {
        return new QuestObjective(
            string(values, "id", "objective"),
            enumValue(ObjectiveType.class, string(values, "type", "PLACEHOLDER"), ObjectiveType.PLACEHOLDER),
            string(values, "target", ""),
            integer(values, "amount", 1),
            stringMap(values.get("data"))
        );
    }

    private QuestReward deserializeReward(Map<?, ?> values) {
        return new QuestReward(
            string(values, "id", "reward"),
            enumValue(RewardType.class, string(values, "type", "COMMAND"), RewardType.COMMAND),
            stringMap(values.get("data"))
        );
    }

    private Set<QuestId> questIdSet(List<String> values) {
        return values.stream()
            .map(QuestId::of)
            .collect(Collectors.toUnmodifiableSet());
    }

    private String string(Map<?, ?> values, String key, String fallback) {
        Object value = values.get(key);
        return value == null ? fallback : String.valueOf(value);
    }

    private int integer(Map<?, ?> values, String key, int fallback) {
        Object value = values.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private Map<String, String> stringMap(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            result.put(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        return Map.copyOf(result);
    }

    private <E extends Enum<E>> E enumValue(Class<E> type, String value, E fallback) {
        try {
            return Enum.valueOf(type, value.toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
