package de.craftplay.quests.stats;

import java.util.UUID;

public record PlayerRankEntry(UUID playerId, int completedQuests, int questPoints, int reputation, int achievements) {
}
