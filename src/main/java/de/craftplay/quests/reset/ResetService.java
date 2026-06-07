package de.craftplay.quests.reset;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.model.QuestType;
import de.craftplay.quests.quest.player.PlayerQuestData;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

public final class ResetService {

    private final CraftplayQuestsPlugin plugin;

    public ResetService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean dailyResetDue(long lastResetMillis) {
        if (!plugin.getConfig().getBoolean("quest-resets.daily.enabled", true)) {
            return false;
        }
        LocalDateTime last = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastResetMillis), ZoneId.systemDefault());
        return last.toLocalDate().isBefore(LocalDateTime.now().toLocalDate());
    }

    public boolean weeklyResetDue(long lastResetMillis) {
        if (!plugin.getConfig().getBoolean("quest-resets.weekly.enabled", true)) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last = LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(lastResetMillis), ZoneId.systemDefault());
        DayOfWeek resetDay = DayOfWeek.valueOf(plugin.getConfig().getString("quest-resets.weekly.day", "SUNDAY"));
        LocalTime resetTime = LocalTime.parse(plugin.getConfig().getString("quest-resets.weekly.time", "23:59"));
        LocalDateTime thisWeeksReset = now.with(resetDay).with(resetTime);
        if (thisWeeksReset.isAfter(now)) {
            thisWeeksReset = thisWeeksReset.minusWeeks(1);
        }
        return last.isBefore(thisWeeksReset);
    }

    public ResetCleanupResult cleanupExpiredQuests(PlayerQuestData data) {
        boolean cancelExpired = plugin.getConfig().getBoolean("quest-resets.expired-quests.cancel-open-quests", true);
        if (!cancelExpired) {
            return new ResetCleanupResult(data, false, false, 0);
        }

        boolean dailyDue = dailyResetDue(data.updatedAt());
        boolean weeklyDue = weeklyResetDue(data.updatedAt());
        if (!dailyDue && !weeklyDue) {
            return new ResetCleanupResult(data, false, false, 0);
        }

        PlayerQuestData updated = data;
        int removed = 0;
        for (QuestId questId : data.activeQuests().keySet()) {
            Optional<Quest> quest = plugin.services().quests().findQuest(questId);
            if (quest.isEmpty()) {
                continue;
            }
            QuestType type = quest.get().metadata().type();
            if ((dailyDue && type == QuestType.DAILY) || (weeklyDue && type == QuestType.WEEKLY)) {
                updated = updated.withoutActiveQuest(questId);
                removed++;
            }
        }

        return new ResetCleanupResult(updated, dailyDue, weeklyDue, removed);
    }

    public record ResetCleanupResult(PlayerQuestData data, boolean dailyReset, boolean weeklyReset, int removedQuests) {
    }
}
