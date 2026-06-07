package de.craftplay.quests.core;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.library.LibraryLoaderService;
import de.craftplay.quests.quest.service.QuestService;
import de.craftplay.quests.scheduler.AsyncTaskService;
import de.craftplay.quests.scheduler.MainThreadService;
import de.craftplay.quests.storage.StorageService;
import java.time.Duration;

public final class ServiceRegistry {

    private final CraftplayQuestsPlugin plugin;
    private MainThreadService mainThreadService;
    private AsyncTaskService asyncTaskService;
    private LibraryLoaderService libraryLoaderService;
    private StorageService storageService;
    private QuestService questService;

    public ServiceRegistry(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        int workers = plugin.getConfig().getInt("performance.async-workers", 4);
        this.mainThreadService = new MainThreadService(plugin);
        this.asyncTaskService = new AsyncTaskService(plugin, mainThreadService, workers);
        this.libraryLoaderService = new LibraryLoaderService(plugin, asyncTaskService);
        this.storageService = new StorageService(plugin, asyncTaskService, libraryLoaderService);
        this.questService = new QuestService(plugin, storageService);

        libraryLoaderService.prepareConfiguredLibraries()
            .handle((report, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().warning("Library preparation failed: " + throwable.getMessage());
                }
                return null;
            })
            .thenCompose(ignored -> storageService.initialize())
            .thenCompose(ignored -> questService.initialize())
            .whenComplete((ignored, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().warning("Quest services failed to initialize: " + throwable.getMessage());
                }
            });
    }

    public void shutdown() {
        if (questService != null) {
            questService.shutdown().join();
        }
        if (storageService != null) {
            storageService.shutdown().join();
        }
        if (asyncTaskService != null) {
            asyncTaskService.shutdown(Duration.ofSeconds(10));
        }
    }

    public MainThreadService mainThread() {
        return mainThreadService;
    }

    public AsyncTaskService asyncTasks() {
        return asyncTaskService;
    }

    public LibraryLoaderService libraries() {
        return libraryLoaderService;
    }

    public StorageService storage() {
        return storageService;
    }

    public QuestService quests() {
        return questService;
    }
}
