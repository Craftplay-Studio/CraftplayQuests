package de.craftplay.quests.hooks;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.player.PlayerQuestData;
import java.lang.reflect.Method;
import java.util.Optional;
import org.bukkit.entity.Player;

public final class PlaceholderService {

    private final CraftplayQuestsPlugin plugin;

    public PlaceholderService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
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
                .replace("%cpquests_title%", data.unlockedTitles().stream().findFirst().orElse(""));
        }
        output = output.replace("%player%", player.getName());
        return applyPlaceholderApi(player, output);
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
