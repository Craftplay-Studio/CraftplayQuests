package de.craftplay.quests.stats;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.player.PlayerQuestData;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class StatsService {

    private final CraftplayQuestsPlugin plugin;
    private volatile StatsSnapshot cachedSnapshot = new StatsSnapshot(0, 0, 0, 0, 0, 0, 0, List.of());
    private volatile long lastRefreshMillis;

    public StatsService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<StatsSnapshot> refresh() {
        return plugin.services().quests().allPlayerData().thenApply(players -> {
            int active = 0;
            int completed = 0;
            int points = 0;
            int reputation = 0;
            int achievements = 0;
            for (PlayerQuestData player : players) {
                active += player.activeQuests().size();
                completed += player.completedQuests().size();
                points += player.questPoints();
                reputation += player.reputation();
                achievements += player.achievements().size();
            }
            List<PlayerRankEntry> topPlayers = players.stream()
                .map(player -> new PlayerRankEntry(
                    player.playerId(),
                    player.completedQuests().size(),
                    player.questPoints(),
                    player.reputation(),
                    player.achievements().size()
                ))
                .sorted(Comparator
                    .comparingInt(PlayerRankEntry::questPoints).reversed()
                    .thenComparing(Comparator.comparingInt(PlayerRankEntry::completedQuests).reversed()))
                .limit(10)
                .toList();

            StatsSnapshot snapshot = new StatsSnapshot(
                plugin.services().quests().registry().size(),
                players.size(),
                active,
                completed,
                points,
                reputation,
                achievements,
                topPlayers
            );
            cachedSnapshot = snapshot;
            lastRefreshMillis = System.currentTimeMillis();
            return snapshot;
        });
    }

    public CompletableFuture<StatsSnapshot> snapshot() {
        int refreshSeconds = Math.max(10, plugin.getConfig().getInt("performance.api-cache-refresh-seconds", 300));
        if (System.currentTimeMillis() - lastRefreshMillis <= refreshSeconds * 1000L) {
            return CompletableFuture.completedFuture(cachedSnapshot);
        }
        return refresh();
    }

    public CompletableFuture<PlayerQuestData> player(UUID playerId) {
        return plugin.services().quests().playerData(playerId);
    }
}
