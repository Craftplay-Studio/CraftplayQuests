package de.craftplay.quests.core;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.achievement.AchievementService;
import de.craftplay.quests.advancement.AdvancementService;
import de.craftplay.quests.api.WebApiService;
import de.craftplay.quests.audit.AuditLogService;
import de.craftplay.quests.bedrock.BedrockService;
import de.craftplay.quests.cache.CacheService;
import de.craftplay.quests.dialog.DialogService;
import de.craftplay.quests.export.ExportService;
import de.craftplay.quests.gui.GuiListener;
import de.craftplay.quests.gui.GuiService;
import de.craftplay.quests.hooks.HookService;
import de.craftplay.quests.hooks.PlaceholderService;
import de.craftplay.quests.importers.ImportService;
import de.craftplay.quests.library.LibraryLoaderService;
import de.craftplay.quests.npc.NpcService;
import de.craftplay.quests.quest.listener.QuestProgressListener;
import de.craftplay.quests.quest.service.QuestService;
import de.craftplay.quests.redis.RedisCacheService;
import de.craftplay.quests.reset.ResetService;
import de.craftplay.quests.scheduler.AsyncTaskService;
import de.craftplay.quests.scheduler.MainThreadService;
import de.craftplay.quests.security.ConfirmService;
import de.craftplay.quests.stats.StatsService;
import de.craftplay.quests.storage.StorageService;
import de.craftplay.quests.title.TitleService;
import de.craftplay.quests.version.MinecraftVersionDetector;
import de.craftplay.quests.version.VersionAdapter;
import java.time.Duration;

public final class ServiceRegistry {

    private final CraftplayQuestsPlugin plugin;
    private MainThreadService mainThreadService;
    private AsyncTaskService asyncTaskService;
    private LibraryLoaderService libraryLoaderService;
    private StorageService storageService;
    private QuestService questService;
    private GuiService guiService;
    private HookService hookService;
    private PlaceholderService placeholderService;
    private RedisCacheService redisCacheService;
    private ConfirmService confirmService;
    private AuditLogService auditLogService;
    private CacheService cacheService;
    private ExportService exportService;
    private StatsService statsService;
    private ResetService resetService;
    private AchievementService achievementService;
    private AdvancementService advancementService;
    private TitleService titleService;
    private NpcService npcService;
    private DialogService dialogService;
    private BedrockService bedrockService;
    private ImportService importService;
    private WebApiService webApiService;
    private VersionAdapter versionAdapter;

    public ServiceRegistry(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        int workers = plugin.getConfig().getInt("performance.async-workers", 4);
        this.mainThreadService = new MainThreadService(plugin);
        this.asyncTaskService = new AsyncTaskService(plugin, mainThreadService, workers);
        this.libraryLoaderService = new LibraryLoaderService(plugin, asyncTaskService);
        this.storageService = new StorageService(plugin, asyncTaskService, libraryLoaderService);
        this.redisCacheService = new RedisCacheService(plugin);
        this.confirmService = new ConfirmService(plugin);
        this.auditLogService = new AuditLogService(plugin);
        this.cacheService = new CacheService(plugin);
        this.exportService = new ExportService(plugin);
        this.hookService = new HookService(plugin);
        this.placeholderService = new PlaceholderService(plugin);
        this.versionAdapter = new MinecraftVersionDetector().detect();
        plugin.getLogger().info("VersionAdapter selected: " + versionAdapter.family() + " (" + versionAdapter.serverVersion() + ")");
        hookService.detect();

        this.resetService = new ResetService(plugin);
        this.achievementService = new AchievementService(plugin);
        this.advancementService = new AdvancementService(plugin);
        this.questService = new QuestService(plugin, storageService, mainThreadService, achievementService);
        this.guiService = new GuiService(plugin);
        this.titleService = new TitleService(plugin);
        this.npcService = new NpcService(plugin);
        this.dialogService = new DialogService(plugin);
        this.bedrockService = new BedrockService(plugin);
        this.importService = new ImportService(plugin);
        this.statsService = new StatsService(plugin);
        this.webApiService = new WebApiService(plugin);

        plugin.getServer().getPluginManager().registerEvents(new QuestProgressListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(new GuiListener(plugin), plugin);
        plugin.getServer().getPluginManager().registerEvents(titleService, plugin);
        placeholderService.registerExpansion();

        libraryLoaderService.prepareConfiguredLibraries()
            .handle((report, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().warning("Library preparation failed: " + throwable.getMessage());
                }
                return null;
            })
            .thenCompose(ignored -> redisCacheService.initialize().exceptionally(throwable -> {
                plugin.getLogger().warning("Redis cache service disabled: " + throwable.getMessage());
                return null;
            }))
            .thenCompose(ignored -> storageService.initialize())
            .thenCompose(ignored -> questService.initialize())
            .thenCompose(ignored -> npcService.initialize())
            .thenCompose(ignored -> advancementService.initialize())
            .thenRun(titleService::start)
            .thenRun(webApiService::start)
            .whenComplete((ignored, throwable) -> {
                if (throwable != null) {
                    plugin.getLogger().warning("Quest services failed to initialize: " + throwable.getMessage());
                }
            });
    }

    public void shutdown() {
        if (webApiService != null) {
            webApiService.stop();
        }
        if (placeholderService != null) {
            placeholderService.unregisterExpansion();
        }
        if (redisCacheService != null) {
            redisCacheService.shutdown().join();
        }
        if (titleService != null) {
            titleService.shutdown();
        }
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

    public GuiService gui() {
        return guiService;
    }

    public HookService hooks() {
        return hookService;
    }

    public PlaceholderService placeholders() {
        return placeholderService;
    }

    public RedisCacheService redis() {
        return redisCacheService;
    }

    public ConfirmService confirmations() {
        return confirmService;
    }

    public AuditLogService audit() {
        return auditLogService;
    }

    public CacheService cache() {
        return cacheService;
    }

    public ExportService exports() {
        return exportService;
    }

    public StatsService stats() {
        return statsService;
    }

    public ResetService resets() {
        return resetService;
    }

    public AchievementService achievements() {
        return achievementService;
    }

    public AdvancementService advancements() {
        return advancementService;
    }

    public TitleService titles() {
        return titleService;
    }

    public NpcService npcs() {
        return npcService;
    }

    public DialogService dialogs() {
        return dialogService;
    }

    public BedrockService bedrock() {
        return bedrockService;
    }

    public ImportService imports() {
        return importService;
    }

    public WebApiService webApi() {
        return webApiService;
    }

    public VersionAdapter version() {
        return versionAdapter;
    }
}
