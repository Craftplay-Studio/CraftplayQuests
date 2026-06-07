package de.craftplay.quests.quest.storage;

import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.player.PlayerQuestData;
import de.craftplay.quests.quest.player.PlayerQuestProgress;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class PlayerQuestDataSerializer {

    public String serialize(PlayerQuestData data) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("player-id", data.playerId().toString());
        configuration.set("updated-at", data.updatedAt());
        configuration.set("completed", data.completedQuests().stream().map(QuestId::value).sorted().toList());
        data.trackedQuest().ifPresent(questId -> configuration.set("tracked", questId.value()));

        for (PlayerQuestProgress progress : data.activeQuests().values()) {
            String path = "active." + progress.questId().value();
            configuration.set(path + ".accepted-at", progress.acceptedAt());
            configuration.set(path + ".objective-progress", progress.objectiveProgress());
        }

        return configuration.saveToString();
    }

    public PlayerQuestData deserialize(UUID fallbackPlayerId, String content) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(content);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid player quest data YAML", exception);
        }

        UUID playerId = Optional.ofNullable(configuration.getString("player-id"))
            .map(UUID::fromString)
            .orElse(fallbackPlayerId);

        Map<QuestId, PlayerQuestProgress> active = new LinkedHashMap<>();
        ConfigurationSection activeSection = configuration.getConfigurationSection("active");
        if (activeSection != null) {
            for (String questIdValue : activeSection.getKeys(false)) {
                QuestId questId = QuestId.of(questIdValue);
                active.put(questId, new PlayerQuestProgress(
                    questId,
                    activeSection.getLong(questIdValue + ".accepted-at", System.currentTimeMillis()),
                    integerMap(activeSection.getConfigurationSection(questIdValue + ".objective-progress"))
                ));
            }
        }

        Optional<QuestId> tracked = Optional.ofNullable(configuration.getString("tracked")).map(QuestId::of);

        return new PlayerQuestData(
            playerId,
            active,
            configuration.getStringList("completed").stream().map(QuestId::of).collect(Collectors.toUnmodifiableSet()),
            tracked,
            configuration.getLong("updated-at", System.currentTimeMillis())
        );
    }

    private Map<String, Integer> integerMap(ConfigurationSection section) {
        if (section == null) {
            return Map.of();
        }

        Map<String, Integer> values = new LinkedHashMap<>();
        for (String key : section.getKeys(false)) {
            values.put(key, section.getInt(key, 0));
        }
        return Map.copyOf(values);
    }
}
