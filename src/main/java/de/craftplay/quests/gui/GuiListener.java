package de.craftplay.quests.gui;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.QuestId;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class GuiListener implements Listener {

    private final CraftplayQuestsPlugin plugin;

    public GuiListener(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof QuestMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        GuiButton button = plugin.services().gui().button(holder.menuId(), event.getRawSlot());
        if (button == null) {
            return;
        }
        handle(player, button.action());
    }

    private void handle(Player player, String action) {
        if ("CLOSE".equalsIgnoreCase(action)) {
            player.closeInventory();
            return;
        }
        if ("OPEN_PLAYER_QUESTS".equalsIgnoreCase(action)) {
            plugin.services().gui().openPlayerQuests(player);
            return;
        }
        if (action.startsWith("OPEN_GUI:player_quests")) {
            plugin.services().gui().openPlayerQuests(player);
            return;
        }
        if (action.startsWith("ACCEPT_QUEST:")) {
            QuestId questId = QuestId.of(action.substring("ACCEPT_QUEST:".length()));
            plugin.services().quests().acceptQuest(player.getUniqueId(), questId)
                .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                    if (throwable != null) {
                        plugin.language().send(player, "errors.generic");
                        return;
                    }
                    plugin.language().send(player, "quest.accepted", Map.of("quest", questId.value()));
                    player.closeInventory();
                }));
        }
    }
}
