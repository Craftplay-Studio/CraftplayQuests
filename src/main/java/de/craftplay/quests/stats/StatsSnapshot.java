package de.craftplay.quests.stats;

import java.util.List;

public record StatsSnapshot(
    int loadedQuests,
    int knownPlayers,
    int activeQuestEntries,
    int completedQuestEntries,
    int questPoints,
    int reputation,
    int achievements,
    List<PlayerRankEntry> topPlayers
) {
}
