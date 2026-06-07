package de.craftplay.quests.storage;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.library.LibraryLoaderService;
import de.craftplay.quests.scheduler.AsyncTaskService;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public final class StorageService {

    private final CraftplayQuestsPlugin plugin;
    private final Map<String, StorageProvider> providers = new HashMap<>();
    private final StorageProvider yamlProvider;
    private volatile StorageProvider activeProvider;

    public StorageService(
        CraftplayQuestsPlugin plugin,
        AsyncTaskService asyncTaskService,
        LibraryLoaderService libraryLoaderService
    ) {
        this.plugin = plugin;
        this.yamlProvider = new YamlStorageProvider(plugin, asyncTaskService);
        register(yamlProvider);
        register(new H2StorageProvider(plugin, asyncTaskService, libraryLoaderService));
    }

    public CompletableFuture<Void> initialize() {
        String requestedType = plugin.getConfig().getString("storage.type", "H2").toUpperCase(Locale.ROOT);
        StorageProvider selectedProvider = providers.getOrDefault(requestedType, yamlProvider);
        activeProvider = selectedProvider;

        return selectedProvider.initialize().handle((ignored, throwable) -> {
            if (throwable == null) {
                plugin.getLogger().info("Storage provider selected: " + selectedProvider.id());
                return CompletableFuture.<Void>completedFuture(null);
            }

            plugin.getLogger().warning("Storage provider " + selectedProvider.id() + " failed: " + throwable.getMessage());
            activeProvider = yamlProvider;
            return yamlProvider.initialize().thenRun(() -> plugin.getLogger().warning("Falling back to YAML storage."));
        }).thenCompose(future -> future);
    }

    public CompletableFuture<Void> shutdown() {
        StorageProvider provider = activeProvider;
        if (provider == null) {
            return CompletableFuture.completedFuture(null);
        }
        return provider.shutdown();
    }

    public CompletableFuture<Optional<String>> loadDocument(StorageDocumentKey key) {
        return provider().loadDocument(key);
    }

    public CompletableFuture<Set<String>> listDocuments(String namespace) {
        return provider().listDocuments(namespace);
    }

    public CompletableFuture<Void> saveDocument(StorageDocumentKey key, String content) {
        return provider().saveDocument(key, content);
    }

    public CompletableFuture<Boolean> deleteDocument(StorageDocumentKey key) {
        return provider().deleteDocument(key);
    }

    public String activeProviderId() {
        StorageProvider provider = activeProvider;
        return provider == null ? "NONE" : provider.id();
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
}
