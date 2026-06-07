package de.craftplay.quests.title;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.player.PlayerQuestData;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class TitleService {

    private final CraftplayQuestsPlugin plugin;

    public TitleService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public Optional<String> selectedTitle(PlayerQuestData data) {
        return data.selectedTitle().or(() -> data.unlockedTitles().stream().findFirst());
    }

    public CompletableFuture<PlayerQuestData> selectTitle(UUID playerId, String title) {
        return plugin.services().quests().playerData(playerId)
            .thenCompose(data -> {
                PlayerQuestData updated = data.withSelectedTitle(Optional.of(title));
                return plugin.services().quests().savePlayerData(updated);
            });
    }

    public CompletableFuture<PlayerQuestData> clearTitle(UUID playerId) {
        return plugin.services().quests().playerData(playerId)
            .thenCompose(data -> plugin.services().quests().savePlayerData(data.withSelectedTitle(Optional.empty())));
    }

    public boolean textDisplaysEnabled() {
        return plugin.getConfig().getBoolean("titles.enabled", true) && plugin.services().version().supportsTextDisplay();
    }
}
