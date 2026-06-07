package de.craftplay.quests.title;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.player.PlayerQuestData;
import java.util.Optional;

public final class TitleService {

    private final CraftplayQuestsPlugin plugin;

    public TitleService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public Optional<String> selectedTitle(PlayerQuestData data) {
        return data.unlockedTitles().stream().findFirst();
    }

    public boolean textDisplaysEnabled() {
        return plugin.getConfig().getBoolean("titles.enabled", true) && plugin.services().version().supportsTextDisplay();
    }
}
