package de.craftplay.quests.achievement;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.player.PlayerQuestData;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class AchievementService {

    private final CraftplayQuestsPlugin plugin;

    public AchievementService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public PlayerQuestData applyQuestCompletionAchievements(PlayerQuestData data, QuestId questId, boolean firstCompletedQuest) {
        PlayerQuestData updated = data;
        if (firstCompletedQuest && !updated.achievements().contains("quest_first")) {
            updated = updated.unlockAchievement("quest_first");
            notifyAchievement(updated, "quest_first");
        }
        String questAchievement = "quest_completed_" + questId.value();
        if (!updated.achievements().contains(questAchievement)) {
            updated = updated.unlockAchievement(questAchievement);
            notifyAchievement(updated, questAchievement);
        }
        return updated;
    }

    public java.util.concurrent.CompletableFuture<PlayerQuestData> unlock(UUID playerId, String achievement) {
        return plugin.services().quests().playerData(playerId).thenCompose(data -> {
            if (data.achievements().contains(achievement)) {
                return java.util.concurrent.CompletableFuture.completedFuture(data);
            }
            PlayerQuestData updated = data.unlockAchievement(achievement);
            notifyAchievement(updated, achievement);
            return plugin.services().quests().savePlayerData(updated);
        });
    }

    private void notifyAchievement(PlayerQuestData data, String achievement) {
        plugin.services().mainThread().execute(() -> {
            Player player = Bukkit.getPlayer(data.playerId());
            if (player == null) {
                return;
            }
            plugin.services().advancements().award(player, achievement);
            plugin.language().send(player, "achievement.unlocked", Map.of("achievement", achievement));
        });
    }
}
