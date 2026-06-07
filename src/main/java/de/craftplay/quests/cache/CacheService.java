package de.craftplay.quests.cache;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

public final class CacheService {

    private final CraftplayQuestsPlugin plugin;

    public CacheService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<CacheInfo> info() {
        return plugin.services().asyncTasks().supplyAsync(() -> new CacheInfo(countFiles(skinsRoot()), countFiles(headsRoot())));
    }

    public CompletableFuture<Void> clearSkins() {
        return clearFolder(skinsRoot());
    }

    public CompletableFuture<Void> clearHeads() {
        return clearFolder(headsRoot());
    }

    public CompletableFuture<Void> clearAll() {
        return clearSkins().thenCompose(ignored -> clearHeads());
    }

    public CompletableFuture<Boolean> delete(String type, String id) {
        return plugin.services().asyncTasks().supplyAsync(() -> {
            Path root = "head".equalsIgnoreCase(type) ? headsRoot() : skinsRoot();
            Path target = root.resolve(id).normalize();
            if (!target.startsWith(root)) {
                return false;
            }
            try {
                return Files.deleteIfExists(target);
            } catch (Exception exception) {
                throw new IllegalStateException("Could not delete cache entry " + id, exception);
            }
        });
    }

    private CompletableFuture<Void> clearFolder(Path folder) {
        return plugin.services().asyncTasks().runAsync(() -> {
            if (Files.notExists(folder)) {
                return;
            }
            try (var stream = Files.walk(folder)) {
                for (Path path : stream.sorted(Comparator.reverseOrder()).toList()) {
                    if (!path.equals(folder)) {
                        Files.deleteIfExists(path);
                    }
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Could not clear cache folder " + folder, exception);
            }
        });
    }

    private int countFiles(Path folder) {
        if (Files.notExists(folder)) {
            return 0;
        }
        try (var stream = Files.walk(folder)) {
            return (int) stream.filter(Files::isRegularFile).count();
        } catch (Exception exception) {
            return 0;
        }
    }

    private Path skinsRoot() {
        return cacheRoot().resolve("skins").normalize();
    }

    private Path headsRoot() {
        return cacheRoot().resolve("heads").normalize();
    }

    private Path cacheRoot() {
        Path dataFolder = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path root = dataFolder.resolve("save").resolve("cache").normalize();
        if (!root.startsWith(dataFolder)) {
            throw new IllegalArgumentException("Cache root must stay inside the plugin data folder.");
        }
        return root;
    }

    public record CacheInfo(int skins, int heads) {
    }
}
