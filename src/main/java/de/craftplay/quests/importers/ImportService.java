package de.craftplay.quests.importers;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class ImportService {

    private final CraftplayQuestsPlugin plugin;

    public ImportService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<String> importQuestsPlugin() {
        return plugin.services().asyncTasks().supplyAsync(() -> {
            Path importFolder = plugin.getDataFolder().toPath().resolve("import");
            Path report = plugin.getDataFolder().toPath().resolve("logs").resolve("questsplugin-import-report.txt");
            try {
                Files.createDirectories(report.getParent());
                Files.writeString(report, "QuestsPlugin import placeholder. Import folder: " + importFolder + System.lineSeparator());
                return report.toString();
            } catch (Exception exception) {
                throw new IllegalStateException("Could not create import report", exception);
            }
        });
    }
}
