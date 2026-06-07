package de.craftplay.quests.dialog;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.QuestId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;

public final class DialogService {

    private final CraftplayQuestsPlugin plugin;
    private final Map<UUID, Long> offers = new ConcurrentHashMap<>();

    public DialogService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void offerQuest(Player player, QuestId questId) {
        offers.put(player.getUniqueId(), System.currentTimeMillis());
        Component message = plugin.language().message("dialog.quest-offer", Map.of("quest", questId.value()))
            .append(Component.space())
            .append(plugin.language().message("dialog.accept-button")
                .clickEvent(ClickEvent.runCommand("/quests accept " + questId.value())))
            .append(Component.space())
            .append(plugin.language().message("dialog.decline-button")
                .clickEvent(ClickEvent.runCommand("/quests cancel " + questId.value())));
        player.sendMessage(message);
    }
}
