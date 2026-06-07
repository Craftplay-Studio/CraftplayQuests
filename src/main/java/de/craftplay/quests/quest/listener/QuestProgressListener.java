package de.craftplay.quests.quest.listener;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.ObjectiveType;
import de.craftplay.quests.quest.model.QuestId;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerFishEvent;

public final class QuestProgressListener implements Listener {

    private final CraftplayQuestsPlugin plugin;

    public QuestProgressListener(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        record(event.getPlayer(), ObjectiveType.BREAK_BLOCK, event.getBlock().getType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        record(event.getPlayer(), ObjectiveType.PLACE_BLOCK, event.getBlockPlaced().getType().name(), 1);
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player player = event.getEntity().getKiller();
        if (player == null) {
            return;
        }
        record(player, ObjectiveType.KILL_MOB, event.getEntityType().name(), 1);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        String target = event.getCaught() instanceof Item item
            ? item.getItemStack().getType().name()
            : "*";
        record(event.getPlayer(), ObjectiveType.FISHING, target, 1);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.services().quests().cleanupExpiredQuestData(player.getUniqueId())
            .whenComplete((result, throwable) -> plugin.services().mainThread().execute(() -> {
                if (throwable != null) {
                    return;
                }
                if (result.removedQuests() > 0 && plugin.getConfig().getBoolean("quest-resets.expired-quests.notify-player", true)) {
                    plugin.language().send(player, "quest.reset.expired-login");
                }
            }));
    }

    private void record(Player player, ObjectiveType type, String target, int amount) {
        UUID playerId = player.getUniqueId();
        plugin.services().quests().recordObjectiveProgress(playerId, type, target, amount)
            .thenAccept(result -> {
                for (QuestId questId : result.completedQuests()) {
                    plugin.services().quests().completeQuest(playerId, questId)
                        .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                            Player onlinePlayer = Bukkit.getPlayer(playerId);
                            if (onlinePlayer == null || throwable != null) {
                                return;
                            }
                            plugin.language().send(onlinePlayer, "quest.completed", java.util.Map.of("quest", questId.value()));
                        }));
                }
            });
    }
}
