package de.craftplay.quests.storage;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.scheduler.AsyncTaskService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public final class YamlStorageProvider implements StorageProvider {

    private static final List<String> DEFAULT_NAMESPACES = List.of(
        "quests",
        "npcs",
        "playerdata",
        "progress",
        "titles",
        "factions",
        "achievements"
    );

    private final CraftplayQuestsPlugin plugin;
    private final AsyncTaskService asyncTaskService;

    public YamlStorageProvider(CraftplayQuestsPlugin plugin, AsyncTaskService asyncTaskService) {
        this.plugin = plugin;
        this.asyncTaskService = asyncTaskService;
    }

    @Override
    public String id() {
        return "YAML";
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return asyncTaskService.runAsync(() -> {
            try {
                Files.createDirectories(rootFolder());
                for (String namespace : DEFAULT_NAMESPACES) {
                    Files.createDirectories(rootFolder().resolve(namespace));
                }
            } catch (IOException exception) {
                throw new IllegalStateException("Could not initialize YAML storage", exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Optional<String>> loadDocument(StorageDocumentKey key) {
        return asyncTaskService.supplyAsync(() -> {
            Path path = pathFor(key);
            if (Files.notExists(path)) {
                return Optional.empty();
            }

            try {
                return Optional.of(Files.readString(path, StandardCharsets.UTF_8));
            } catch (IOException exception) {
                throw new IllegalStateException("Could not load YAML document " + key, exception);
            }
        });
    }

    @Override
    public CompletableFuture<Set<String>> listDocuments(String namespace) {
        return asyncTaskService.supplyAsync(() -> {
            Path folder = rootFolder().resolve(new StorageDocumentKey(namespace, "validation").namespace()).normalize();
            if (Files.notExists(folder)) {
                return Set.of();
            }

            try (var stream = Files.list(folder)) {
                return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yml"))
                    .map(path -> path.getFileName().toString())
                    .map(fileName -> fileName.substring(0, fileName.length() - 4))
                    .collect(Collectors.toUnmodifiableSet());
            } catch (IOException exception) {
                throw new IllegalStateException("Could not list YAML namespace " + namespace, exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveDocument(StorageDocumentKey key, String content) {
        return asyncTaskService.runAsync(() -> {
            Path path = pathFor(key);
            try {
                Files.createDirectories(path.getParent());
                Files.writeString(path, content, StandardCharsets.UTF_8);
            } catch (IOException exception) {
                throw new IllegalStateException("Could not save YAML document " + key, exception);
            }
        });
    }

    @Override
    public CompletableFuture<Boolean> deleteDocument(StorageDocumentKey key) {
        return asyncTaskService.supplyAsync(() -> {
            try {
                return Files.deleteIfExists(pathFor(key));
            } catch (IOException exception) {
                throw new IllegalStateException("Could not delete YAML document " + key, exception);
            }
        });
    }

    private Path pathFor(StorageDocumentKey key) {
        return rootFolder().resolve(key.namespace()).resolve(key.fileName()).normalize();
    }

    private Path rootFolder() {
        String configured = plugin.getConfig().getString("storage.yaml.folder", "save/yaml");
        Path dataFolder = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path folder = dataFolder.resolve(configured).normalize();
        if (!folder.startsWith(dataFolder)) {
            throw new IllegalArgumentException("YAML storage folder must stay inside the plugin data folder.");
        }
        return folder;
    }
}
