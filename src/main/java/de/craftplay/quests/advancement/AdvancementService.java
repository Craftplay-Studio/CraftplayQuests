package de.craftplay.quests.advancement;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

public final class AdvancementService {

    private final CraftplayQuestsPlugin plugin;
    private final Set<String> loaded = ConcurrentHashMap.newKeySet();

    public AdvancementService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> initialize() {
        return generateAdvancementAssets().thenCompose(ignored -> plugin.services().mainThread().supply(() -> {
            load("root", rootAdvancementJson());
            load("quest_first", achievementJson(
                plugin.getConfig().getString("achievements.quest_first.title", "Der Anfang"),
                plugin.getConfig().getString("achievements.quest_first.description", "Schließe deine erste Quest ab."),
                materialKey(plugin.getConfig().getString("achievements.quest_first.icon.material", "minecraft:writable_book")),
                "root"
            ));
            return null;
        }));
    }

    public CompletableFuture<Void> generateAdvancementAssets() {
        if (!plugin.getConfig().getBoolean("achievements.advancement-tab.enabled", true)) {
            return CompletableFuture.completedFuture(null);
        }

        return plugin.services().asyncTasks().runAsync(() -> {
            try {
                Path root = rootFolder();
                Files.createDirectories(root);
                Files.writeString(root.resolve("root.json"), rootAdvancementJson());
                Files.writeString(root.resolve("quest_first.json"), achievementJson(
                    plugin.getConfig().getString("achievements.quest_first.title", "Der Anfang"),
                    plugin.getConfig().getString("achievements.quest_first.description", "Schließe deine erste Quest ab."),
                    materialKey(plugin.getConfig().getString("achievements.quest_first.icon.material", "minecraft:writable_book")),
                    "root"
                ));
            } catch (Exception exception) {
                throw new IllegalStateException("Could not generate advancement assets", exception);
            }
        });
    }

    public CompletableFuture<Void> writeAchievementAsset(String achievementId, String title, String description, String icon) {
        if (!plugin.getConfig().getBoolean("achievements.advancement-tab.enabled", true)) {
            return CompletableFuture.completedFuture(null);
        }

        return plugin.services().asyncTasks().runAsync(() -> {
            try {
                Path root = rootFolder();
                Files.createDirectories(root);
                Files.writeString(root.resolve(safeId(achievementId) + ".json"), achievementJson(title, description, materialKey(icon), "root"));
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not write advancement asset " + achievementId + ": " + exception.getMessage());
            }
        });
    }

    public CompletableFuture<Void> registerAchievement(String achievementId, String title, String description, String icon) {
        return writeAchievementAsset(achievementId, title, description, icon)
            .thenCompose(ignored -> plugin.services().mainThread().supply(() -> {
                load("root", rootAdvancementJson());
                load(safeId(achievementId), achievementJson(title, description, materialKey(icon), "root"));
                return null;
            }));
    }

    public void award(Player player, String achievementId) {
        if (player == null || achievementId == null || achievementId.isBlank()
            || !plugin.getConfig().getBoolean("achievements.advancement-tab.enabled", true)) {
            return;
        }

        String safeId = safeId(achievementId);
        String title = plugin.getConfig().getString("achievements." + safeId + ".title", achievementId);
        String description = plugin.getConfig().getString("achievements." + safeId + ".description", achievementId);
        String icon = materialKey(plugin.getConfig().getString("achievements." + safeId + ".icon.material", "minecraft:book"));

        load("root", rootAdvancementJson());
        load(safeId, achievementJson(title, description, icon, "root"));

        Advancement advancement = Bukkit.getAdvancement(key(safeId));
        if (advancement == null) {
            return;
        }

        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            progress.awardCriteria(criterion);
        }
    }

    public Map<String, Boolean> loadedSnapshot() {
        return loaded.stream()
            .collect(Collectors.toUnmodifiableMap(id -> id, id -> Bukkit.getAdvancement(key(id)) != null));
    }

    private void load(String id, String json) {
        String safeId = safeId(id);
        if (loaded.contains(safeId) && Bukkit.getAdvancement(key(safeId)) != null) {
            return;
        }

        try {
            Object unsafe = Bukkit.getUnsafe();
            unsafe.getClass().getMethod("loadAdvancement", NamespacedKey.class, String.class)
                .invoke(unsafe, key(safeId), json);
            loaded.add(safeId);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("Could not load advancement " + safeId + ": " + exception.getMessage());
        } catch (IllegalArgumentException exception) {
            loaded.add(safeId);
        }
    }

    private Path rootFolder() {
        return plugin.getDataFolder().toPath()
            .resolve("save")
            .resolve("exports")
            .resolve("datapack")
            .resolve("data")
            .resolve("craftplayquests")
            .resolve("advancements");
    }

    private String rootAdvancementJson() {
        String title = plugin.getConfig().getString("achievements.advancement-tab.title", "Craftplay Quests");
        String background = plugin.getConfig().getString("achievements.advancement-tab.background", "minecraft:textures/block/deepslate_tiles.png");
        String icon = materialKey(plugin.getConfig().getString("achievements.advancement-tab.icon.material", "minecraft:book"));
        return """
            {
              "display": {
                "icon": { "id": "%s" },
                "title": "%s",
                "description": "CraftplayQuests",
                "background": "%s",
                "show_toast": false,
                "announce_to_chat": false
              },
              "criteria": {
                "tick": { "trigger": "minecraft:tick" }
              }
            }
            """.formatted(escape(icon), escape(title), escape(background));
    }

    private String achievementJson(String title, String description, String icon, String parent) {
        return """
            {
              "parent": "craftplayquests:%s",
              "display": {
                "icon": { "id": "%s" },
                "title": "%s",
                "description": "%s",
                "show_toast": true,
                "announce_to_chat": true
              },
              "criteria": {
                "manual": { "trigger": "minecraft:impossible" }
              }
            }
            """.formatted(escape(parent), escape(icon), escape(title), escape(description));
    }

    private NamespacedKey key(String id) {
        return new NamespacedKey(plugin, safeId(id));
    }

    private String safeId(String id) {
        return id.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9_./-]", "_");
    }

    private String materialKey(String material) {
        String value = material == null || material.isBlank() ? "minecraft:book" : material.toLowerCase(java.util.Locale.ROOT);
        if (!value.contains(":")) {
            value = "minecraft:" + value;
        }
        return value;
    }

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
