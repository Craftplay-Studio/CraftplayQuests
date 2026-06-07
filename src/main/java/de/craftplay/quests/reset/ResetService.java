package de.craftplay.quests.reset;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

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
}
