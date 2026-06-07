package de.craftplay.quests.storage;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.library.LibraryLoaderService;
import de.craftplay.quests.scheduler.AsyncTaskService;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class StorageService {

    private final CraftplayQuestsPlugin plugin;
    private final Map<String, StorageProvider> providers = new HashMap<>();
    private final Map<StorageDocumentKey, String> dirtyQueue = new java.util.concurrent.ConcurrentHashMap<>();
    private final StorageProvider yamlProvider;
    private volatile StorageProvider activeProvider;
    private ScheduledExecutorService dirtyQueueExecutor;

    public StorageService(
        CraftplayQuestsPlugin plugin,
        AsyncTaskService asyncTaskService,
        LibraryLoaderService libraryLoaderService
    ) {
        this.plugin = plugin;
        this.yamlProvider = new YamlStorageProvider(plugin, asyncTaskService);
        register(yamlProvider);
        register(new H2StorageProvider(plugin, asyncTaskService, libraryLoaderService));
        register(new MySqlStorageProvider(plugin, asyncTaskService, libraryLoaderService));
        register(new MariaDbStorageProvider(plugin, asyncTaskService, libraryLoaderService));
    }

    public CompletableFuture<Void> initialize() {
        String requestedType = plugin.getConfig().getString("storage.type", "H2").toUpperCase(Locale.ROOT);
        StorageProvider selectedProvider = providers.getOrDefault(requestedType, yamlProvider);
        activeProvider = selectedProvider;

        return selectedProvider.initialize().handle((ignored, throwable) -> {
            if (throwable == null) {
                plugin.getLogger().info("Storage provider selected: " + selectedProvider.id());
                startDirtyQueue();
                return CompletableFuture.<Void>completedFuture(null);
            }

            plugin.getLogger().warning("Storage provider " + selectedProvider.id() + " failed: " + throwable.getMessage());
            activeProvider = yamlProvider;
            return yamlProvider.initialize().thenRun(() -> {
                plugin.getLogger().warning("Falling back to YAML storage.");
                startDirtyQueue();
            });
        }).thenCompose(future -> future);
    }

    public CompletableFuture<Void> shutdown() {
        stopDirtyQueue();
        StorageProvider provider = activeProvider;
        if (provider == null) {
            return CompletableFuture.completedFuture(null);
        }
        return flushDirtyQueue().thenCompose(ignored -> provider.shutdown());
    }

    public CompletableFuture<Optional<String>> loadDocument(StorageDocumentKey key) {
        String queued = dirtyQueue.get(key);
        if (queued != null) {
            return CompletableFuture.completedFuture(Optional.of(queued));
        }
        return provider().loadDocument(key);
    }

    public CompletableFuture<Set<String>> listDocuments(String namespace) {
        return provider().listDocuments(namespace);
    }

    public CompletableFuture<Void> saveDocument(StorageDocumentKey key, String content) {
        if (plugin.getConfig().getBoolean("performance.dirty-queue.enabled", true)) {
            dirtyQueue.put(key, content);
            int batchSize = Math.max(1, plugin.getConfig().getInt("performance.database-batch-size", 100));
            if (dirtyQueue.size() >= batchSize) {
                return flushDirtyQueue();
            }
            return CompletableFuture.completedFuture(null);
        }
        return provider().saveDocument(key, content);
    }

    public CompletableFuture<Boolean> deleteDocument(StorageDocumentKey key) {
        dirtyQueue.remove(key);
        return provider().deleteDocument(key);
    }

    public String activeProviderId() {
        StorageProvider provider = activeProvider;
        return provider == null ? "NONE" : provider.id();
    }

    public int queuedWrites() {
        return dirtyQueue.size();
    }

    public CompletableFuture<Void> flushDirtyQueue() {
        if (dirtyQueue.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Map<StorageDocumentKey, String> batch = new LinkedHashMap<>();
        for (Map.Entry<StorageDocumentKey, String> entry : dirtyQueue.entrySet()) {
            if (dirtyQueue.remove(entry.getKey(), entry.getValue())) {
                batch.put(entry.getKey(), entry.getValue());
            }
        }

        if (batch.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        StorageProvider provider = provider();
        return CompletableFuture.allOf(batch.entrySet().stream()
            .map(entry -> provider.saveDocument(entry.getKey(), entry.getValue()))
            .toArray(CompletableFuture[]::new));
    }

    private StorageProvider provider() {
        StorageProvider provider = activeProvider;
        if (provider == null) {
            return yamlProvider;
        }
        return provider;
    }

    private void register(StorageProvider provider) {
        providers.put(provider.id().toUpperCase(Locale.ROOT), provider);
    }

    private void startDirtyQueue() {
        if (!plugin.getConfig().getBoolean("performance.dirty-queue.enabled", true) || dirtyQueueExecutor != null) {
            return;
        }
        int interval = Math.max(5, plugin.getConfig().getInt("performance.dirty-queue.flush-interval-seconds", 30));
        dirtyQueueExecutor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "craftplayquests-dirty-queue");
            thread.setDaemon(true);
            return thread;
        });
        dirtyQueueExecutor.scheduleWithFixedDelay(() -> flushDirtyQueue().exceptionally(throwable -> {
            plugin.getLogger().warning("DirtyQueue flush failed: " + throwable.getMessage());
            return null;
        }), interval, interval, TimeUnit.SECONDS);
    }

    private void stopDirtyQueue() {
        ScheduledExecutorService executor = dirtyQueueExecutor;
        dirtyQueueExecutor = null;
        if (executor != null) {
            executor.shutdownNow();
        }
    }
}
