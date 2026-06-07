package de.craftplay.quests.export;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class ExportService {

    private final CraftplayQuestsPlugin plugin;

    public ExportService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Path> exportSaveData() {
        return zip("save", "save/exports", "craftplayquests-export");
    }

    public CompletableFuture<Path> backupSaveData() {
        return zip("save", "save/backups", "craftplayquests-backup");
    }

    private CompletableFuture<Path> zip(String sourceFolder, String targetFolder, String prefix) {
        return plugin.services().storage().flushDirtyQueue()
            .thenCompose(ignored -> plugin.services().asyncTasks().supplyAsync(() -> {
                try {
                    Path dataFolder = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
                    Path source = dataFolder.resolve(sourceFolder).normalize();
                    Path target = dataFolder.resolve(targetFolder).normalize();
                    Files.createDirectories(target);
                    Path zipFile = target.resolve(prefix + "-" + DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(':', '-') + ".zip");
                    try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(zipFile))) {
                        if (Files.exists(source)) {
                            try (var stream = Files.walk(source)) {
                                for (Path path : stream.filter(Files::isRegularFile).toList()) {
                                    String entryName = source.relativize(path).toString().replace('\\', '/');
                                    zip.putNextEntry(new ZipEntry(entryName));
                                    Files.copy(path, zip);
                                    zip.closeEntry();
                                }
                            }
                        }
                    }
                    return zipFile;
                } catch (Exception exception) {
                    throw new IllegalStateException("Could not create export", exception);
                }
            }));
    }
}
