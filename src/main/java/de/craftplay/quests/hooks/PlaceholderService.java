package de.craftplay.quests.hooks;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.player.PlayerQuestData;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class PlaceholderService {

    private final CraftplayQuestsPlugin plugin;
    private CraftplayPlaceholderExpansion expansion;

    public PlaceholderService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void registerExpansion() {
        if (!plugin.getConfig().getBoolean("placeholderapi.expansion-enabled", true)
            || !plugin.services().hooks().enabled("PlaceholderAPI")) {
            return;
        }

        try {
            this.expansion = new CraftplayPlaceholderExpansion(plugin);
            if (expansion.register()) {
                plugin.getLogger().info("PlaceholderAPI expansion registered: " + expansion.getIdentifier());
            }
        } catch (NoClassDefFoundError | IllegalStateException exception) {
            this.expansion = null;
            plugin.getLogger().warning("Could not register PlaceholderAPI expansion: " + exception.getMessage());
        }
    }

    public void unregisterExpansion() {
        CraftplayPlaceholderExpansion current = expansion;
        expansion = null;
        if (current != null) {
            current.unregister();
        }
    }

    public String apply(Player player, String input) {
        String output = input == null ? "" : input;
        Optional<PlayerQuestData> cached = plugin.services().quests().cachedPlayerData(player.getUniqueId());
        if (cached.isPresent()) {
            PlayerQuestData data = cached.get();
            output = output
                .replace("%cpquests_completed%", String.valueOf(data.completedQuests().size()))
                .replace("%cpquests_active%", String.valueOf(data.activeQuests().size()))
                .replace("%cpquests_points%", String.valueOf(data.questPoints()))
                .replace("%cpquests_reputation%", String.valueOf(data.reputation()))
                .replace("%cpquests_achievements%", String.valueOf(data.achievements().size()))
                .replace("%cpquests_tracked%", data.trackedQuest().map(de.craftplay.quests.quest.model.QuestId::value).orElse(""))
                .replace("%cpquests_title%", plugin.services().titles().selectedTitle(data).orElse(""));
        }
        output = output.replace("%player%", player.getName());
        return applyPlaceholderApi(player, output);
    }

    public String value(UUID playerId, String key) {
        if (key == null) {
            return "";
        }

        String normalized = key.trim().toLowerCase(java.util.Locale.ROOT);
        if ("server".equals(normalized) || "server_id".equals(normalized)) {
            return plugin.serverSettings().serverId();
        }
        if ("server_name".equals(normalized) || "display_name".equals(normalized)) {
            return plugin.serverSettings().displayName();
        }
        if ("storage".equals(normalized)) {
            return plugin.services().storage().activeProviderId();
        }
        if ("quests_total".equals(normalized)) {
            return String.valueOf(plugin.services().quests().registry().size());
        }
        if ("api_running".equals(normalized)) {
            return String.valueOf(plugin.services().webApi().running());
        }
        if (playerId == null) {
            return "";
        }

        Optional<PlayerQuestData> cached = plugin.services().quests().cachedPlayerData(playerId);
        if (cached.isEmpty()) {
            return "";
        }

        PlayerQuestData data = cached.get();
        return switch (normalized) {
            case "completed", "completed_count" -> String.valueOf(data.completedQuests().size());
            case "active", "active_count" -> String.valueOf(data.activeQuests().size());
            case "points", "quest_points" -> String.valueOf(data.questPoints());
            case "reputation" -> String.valueOf(data.reputation());
            case "achievements", "achievements_count" -> String.valueOf(data.achievements().size());
            case "titles", "titles_count" -> String.valueOf(data.unlockedTitles().size());
            case "title", "selected_title" -> plugin.services().titles().selectedTitle(data).orElse("");
            case "tracked", "tracked_quest" -> data.trackedQuest().map(de.craftplay.quests.quest.model.QuestId::value).orElse("");
            default -> "";
        };
    }

    private String applyPlaceholderApi(Player player, String input) {
        if (!plugin.services().hooks().enabled("PlaceholderAPI")) {
            return input;
        }
        try {
            Class<?> placeholderApi = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            Method setPlaceholders = placeholderApi.getMethod("setPlaceholders", Player.class, String.class);
            Object result = setPlaceholders.invoke(null, player, input);
            return String.valueOf(result);
        } catch (ReflectiveOperationException exception) {
            plugin.getLogger().warning("PlaceholderAPI reflection failed: " + exception.getMessage());
            return input;
        }
    }
}
