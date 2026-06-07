package de.craftplay.quests.importers;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.ObjectiveType;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestDifficulty;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.model.QuestMetadata;
import de.craftplay.quests.quest.model.QuestObjective;
import de.craftplay.quests.quest.model.QuestType;
import de.craftplay.quests.quest.storage.QuestSerializer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ImportService {

    private final CraftplayQuestsPlugin plugin;
    private final QuestSerializer questSerializer = new QuestSerializer();

    public ImportService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<String> importQuestsPlugin() {
        return plugin.services().exports().backupSaveData().thenCompose(backup -> plugin.services().asyncTasks().supplyAsync(() -> {
            Path importFolder = plugin.getDataFolder().toPath().resolve("import");
            Path report = plugin.getDataFolder().toPath().resolve("logs").resolve("questsplugin-import-report.txt");
            try {
                Files.createDirectories(report.getParent());
                Path source = importFolder.resolve("questsplugin");
                Files.createDirectories(source);

                int imported = 0;
                int skipped = 0;
                StringBuilder reportContent = new StringBuilder();
                reportContent.append("CraftplayQuests QuestsPlugin import").append(System.lineSeparator());
                reportContent.append("Backup: ").append(backup).append(System.lineSeparator());
                reportContent.append("Source: ").append(source).append(System.lineSeparator()).append(System.lineSeparator());

                try (var stream = Files.walk(source, 1)) {
                    for (Path file : stream.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".yml")).toList()) {
                        try {
                            Quest quest = parseQuestFile(file);
                            plugin.services().quests().saveQuest(quest).join();
                            imported++;
                            reportContent.append("IMPORTED ").append(file.getFileName()).append(" -> ").append(quest.id().value()).append(System.lineSeparator());
                        } catch (Exception exception) {
                            skipped++;
                            reportContent.append("SKIPPED ").append(file.getFileName()).append(": ").append(exception.getMessage()).append(System.lineSeparator());
                        }
                    }
                }

                reportContent.append(System.lineSeparator())
                    .append("Imported: ").append(imported).append(System.lineSeparator())
                    .append("Skipped: ").append(skipped).append(System.lineSeparator());
                Files.writeString(report, reportContent.toString());
                return report.toString();
            } catch (Exception exception) {
                throw new IllegalStateException("Could not create import report", exception);
            }
        }));
    }

    private Quest parseQuestFile(Path file) throws Exception {
        String content = Files.readString(file);
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.loadFromString(content);

        if (configuration.contains("objectives") && configuration.contains("rewards")) {
            String withId = configuration.contains("id")
                ? content
                : "id: \"" + fallbackQuestId(file) + "\"" + System.lineSeparator() + content;
            return questSerializer.deserialize(withId);
        }

        String id = configuration.getString("id", fallbackQuestId(file));
        String name = configuration.getString("name", configuration.getString("display-name", id));
        String category = configuration.getString("category", "imported");
        ObjectiveType objectiveType = objectiveType(configuration.getString("type", configuration.getString("objective.type", "PLACEHOLDER")));
        String target = configuration.getString("target", configuration.getString("objective.target", "*"));
        int amount = Math.max(1, configuration.getInt("amount", configuration.getInt("objective.amount", 1)));

        return new Quest(
            QuestId.of(id),
            name,
            configuration.getStringList("description"),
            new QuestMetadata(category, QuestType.NORMAL, QuestDifficulty.NORMAL, 1, 1, "", Optional.empty(), true),
            Set.of(),
            Set.of(),
            List.of(),
            List.of(new QuestObjective("imported_objective", objectiveType, target, amount, java.util.Map.of())),
            List.of()
        );
    }

    private ObjectiveType objectiveType(String value) {
        try {
            return ObjectiveType.valueOf(value.toUpperCase(java.util.Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException exception) {
            return ObjectiveType.PLACEHOLDER;
        }
    }

    private String fallbackQuestId(Path file) {
        String fileName = file.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }
}
