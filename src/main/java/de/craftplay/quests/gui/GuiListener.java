package de.craftplay.quests.gui;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.QuestId;
import java.util.Map;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

public final class GuiListener implements Listener {

    private final CraftplayQuestsPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

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

        GuiButton button = holder.button(event.getRawSlot());
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
        if ("BACK".equalsIgnoreCase(action) || "OPEN_GUI:main".equalsIgnoreCase(action)) {
            plugin.services().gui().openMain(player);
            return;
        }
        if ("OPEN_PLAYER_QUESTS".equalsIgnoreCase(action)) {
            plugin.services().gui().openPlayerQuests(player);
            return;
        }
        if ("OPEN_GUI:adventure_book".equalsIgnoreCase(action) || "OPEN_ADVENTURE_BOOK".equalsIgnoreCase(action)) {
            plugin.services().gui().openAdventureBook(player);
            return;
        }
        if ("OPEN_GUI:achievements".equalsIgnoreCase(action) || "OPEN_ACHIEVEMENTS".equalsIgnoreCase(action)) {
            plugin.services().gui().openAchievements(player);
            return;
        }
        if ("OPEN_GUI:admin_main".equalsIgnoreCase(action) || "OPEN_ADMIN".equalsIgnoreCase(action)) {
            if (player.hasPermission(de.craftplay.quests.util.Permissions.ADMIN)) {
                plugin.services().gui().openAdmin(player);
            }
            return;
        }
        if ("OPEN_GUI:admin_quests".equalsIgnoreCase(action)) {
            if (player.hasPermission(de.craftplay.quests.util.Permissions.ADMIN_QUESTS)) {
                plugin.services().gui().openAdminQuests(player);
            }
            return;
        }
        if ("OPEN_GUI:admin_npcs".equalsIgnoreCase(action)) {
            if (player.hasPermission(de.craftplay.quests.util.Permissions.ADMIN_NPCS)) {
                plugin.services().gui().openAdminNpcs(player);
            }
            return;
        }
        if ("OPEN_GUI:admin_categories".equalsIgnoreCase(action)) {
            if (player.hasPermission(de.craftplay.quests.util.Permissions.ADMIN_CATEGORIES)) {
                plugin.services().gui().openAdminCategories(player);
            }
            return;
        }
        if ("OPEN_GUI:admin_titles".equalsIgnoreCase(action)) {
            if (player.hasPermission(de.craftplay.quests.util.Permissions.ADMIN_TITLES)) {
                plugin.services().gui().openAdminTitles(player);
            }
            return;
        }
        if ("OPEN_GUI:admin_settings".equalsIgnoreCase(action)) {
            if (player.hasPermission(de.craftplay.quests.util.Permissions.ADMIN_RELOAD)) {
                plugin.services().gui().openAdminSettings(player);
            }
            return;
        }
        if (action.startsWith("OPEN_GUI:player_quests")) {
            plugin.services().gui().openPlayerQuests(player);
            return;
        }
        if (action.startsWith("ADMIN_QUEST_INFO:")) {
            QuestId questId = QuestId.of(action.substring("ADMIN_QUEST_INFO:".length()));
            plugin.services().quests().findQuest(questId).ifPresent(quest -> plugin.services().gui().openQuestDetails(player, quest));
            return;
        }
        if (action.startsWith("ADMIN_NPC_INFO:")) {
            String npcId = action.substring("ADMIN_NPC_INFO:".length());
            plugin.services().npcs().find(npcId).ifPresent(npc -> plugin.language().send(player, "commands.npc-list-entry", Map.of(
                "npc", npc.id(),
                "name", npc.displayName(),
                "quests", String.valueOf(npc.quests().size())
            )));
            return;
        }
        if (action.startsWith("ADMIN_GRANT_TITLE:")) {
            String title = action.substring("ADMIN_GRANT_TITLE:".length());
            plugin.services().titles().grantTitle(player.getUniqueId(), title)
                .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() ->
                    plugin.language().send(player, throwable == null ? "title.unlocked" : "errors.generic", Map.of("title", title))));
            return;
        }
        if ("CREATE_QUEST".equalsIgnoreCase(action)) {
            if (player.hasPermission(de.craftplay.quests.util.Permissions.ADMIN_QUESTS)) {
                plugin.services().gui().createTemplateQuest(player);
            }
            return;
        }
        if ("CREATE_NPC".equalsIgnoreCase(action)) {
            if (player.hasPermission(de.craftplay.quests.util.Permissions.ADMIN_NPCS)) {
                plugin.services().gui().createTemplateNpc(player);
            }
            return;
        }
        if ("CREATE_TITLE".equalsIgnoreCase(action)) {
            if (player.hasPermission(de.craftplay.quests.util.Permissions.ADMIN_TITLES)) {
                plugin.services().gui().createAdminTitle(player);
            }
            return;
        }
        if ("OPEN_IMPORT".equalsIgnoreCase(action)) {
            plugin.language().send(player, "admin.editor-not-ready");
            return;
        }
        if ("OPEN_CACHE_ADMIN".equalsIgnoreCase(action)) {
            player.performCommand("quests cache info");
            return;
        }
        if ("OPEN_SYSTEM_ADMIN".equalsIgnoreCase(action)) {
            player.performCommand("quests debug services");
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
            return;
        }
        if (action.startsWith("CANCEL_QUEST:") || action.startsWith("DECLINE_QUEST:")) {
            QuestId questId = QuestId.of(action.substring(action.indexOf(':') + 1));
            plugin.services().quests().cancelQuest(player.getUniqueId(), questId)
                .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                    if (throwable != null) {
                        plugin.language().send(player, "errors.generic");
                        return;
                    }
                    plugin.language().send(player, "quest.canceled", Map.of("quest", questId.value()));
                    player.closeInventory();
                }));
            return;
        }
        if (action.startsWith("TRACK_QUEST:")) {
            QuestId questId = QuestId.of(action.substring("TRACK_QUEST:".length()));
            plugin.services().quests().trackQuest(player.getUniqueId(), questId)
                .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                    if (throwable != null) {
                        plugin.language().send(player, "errors.generic");
                        return;
                    }
                    plugin.language().send(player, "quest.tracked", Map.of("quest", questId.value()));
                }));
            return;
        }
        if ("UNTRACK_QUEST".equalsIgnoreCase(action)) {
            plugin.services().quests().untrackQuest(player.getUniqueId())
                .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> plugin.language().send(player, throwable == null ? "quest.untracked" : "errors.generic")));
            return;
        }
        if (action.startsWith("PLAYER_COMMAND:")) {
            player.performCommand(applyPlayer(action.substring("PLAYER_COMMAND:".length()), player));
            return;
        }
        if (action.startsWith("CONSOLE_COMMAND:")) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), applyPlayer(action.substring("CONSOLE_COMMAND:".length()), player));
            return;
        }
        if (action.startsWith("MESSAGE:")) {
            player.sendMessage(miniMessage.deserialize(plugin.services().placeholders().apply(player, action.substring("MESSAGE:".length()))));
            return;
        }
        if (action.startsWith("SOUND:")) {
            try {
                player.playSound(player.getLocation(), Sound.valueOf(action.substring("SOUND:".length()).toUpperCase(java.util.Locale.ROOT)), 1.0F, 1.0F);
            } catch (IllegalArgumentException ignored) {
                plugin.language().send(player, "errors.generic");
            }
        }
    }

    private String applyPlayer(String input, Player player) {
        return plugin.services().placeholders().apply(player, input)
            .replace("{player}", player.getName())
            .replace("{uuid}", player.getUniqueId().toString());
    }
}
