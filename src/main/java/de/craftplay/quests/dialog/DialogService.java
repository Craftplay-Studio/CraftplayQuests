package de.craftplay.quests.dialog;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.QuestId;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

public final class DialogService {

    private final CraftplayQuestsPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final Map<UUID, Long> offers = new ConcurrentHashMap<>();

    public DialogService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void offerQuest(Player player, QuestId questId) {
        offers.put(player.getUniqueId(), System.currentTimeMillis());
        player.sendMessage(miniMessage.deserialize("<gold>Quest: <yellow>" + questId.value() + "</yellow> ")
            .append(miniMessage.deserialize("<green>[Annehmen]</green>")
                .clickEvent(ClickEvent.runCommand("/quests accept " + questId.value())))
            .append(miniMessage.deserialize(" "))
            .append(miniMessage.deserialize("<red>[Ablehnen]</red>")
                .clickEvent(ClickEvent.runCommand("/quests cancel " + questId.value()))));
    }
}
