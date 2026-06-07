package de.craftplay.quests.advancement;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public final class AdvancementService {

    private final CraftplayQuestsPlugin plugin;

    public AdvancementService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> generateAdvancementAssets() {
        if (!plugin.getConfig().getBoolean("achievements.advancement-tab.enabled", true)) {
            return CompletableFuture.completedFuture(null);
        }

        return plugin.services().asyncTasks().runAsync(() -> {
            try {
                Path root = plugin.getDataFolder().toPath()
                    .resolve("save")
                    .resolve("exports")
                    .resolve("datapack")
                    .resolve("data")
                    .resolve("craftplayquests")
                    .resolve("advancements");
                Files.createDirectories(root);
                Files.writeString(root.resolve("root.json"), rootAdvancementJson());
                Files.writeString(root.resolve("quest_first.json"), achievementJson(
                    "Der Anfang",
                    "Schließe deine erste Quest ab.",
                    "minecraft:writable_book",
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
                Path root = plugin.getDataFolder().toPath()
                    .resolve("save")
                    .resolve("exports")
                    .resolve("datapack")
                    .resolve("data")
                    .resolve("craftplayquests")
                    .resolve("advancements");
                Files.createDirectories(root);
                Files.writeString(root.resolve(achievementId + ".json"), achievementJson(title, description, icon, "root"));
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not write advancement asset " + achievementId + ": " + exception.getMessage());
            }
        });
    }

    private String rootAdvancementJson() {
        String title = plugin.getConfig().getString("achievements.advancement-tab.title", "Craftplay Quests");
        String background = plugin.getConfig().getString("achievements.advancement-tab.background", "minecraft:textures/block/deepslate_tiles.png");
        String icon = plugin.getConfig().getString("achievements.advancement-tab.icon.material", "minecraft:book").toLowerCase(java.util.Locale.ROOT);
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

    private String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
