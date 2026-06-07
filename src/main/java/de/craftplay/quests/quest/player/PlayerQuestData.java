package de.craftplay.quests.quest.player;

import de.craftplay.quests.quest.model.QuestId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public record PlayerQuestData(
    UUID playerId,
    Map<QuestId, PlayerQuestProgress> activeQuests,
    Set<QuestId> completedQuests,
    Optional<QuestId> trackedQuest,
    int questPoints,
    int reputation,
    Set<String> unlockedTitles,
    Set<String> achievements,
    long updatedAt
) {

    public PlayerQuestData {
        if (playerId == null) {
            throw new IllegalArgumentException("Player id must not be null.");
        }
        activeQuests = activeQuests == null ? Map.of() : Map.copyOf(activeQuests);
        completedQuests = completedQuests == null ? Set.of() : Set.copyOf(completedQuests);
        trackedQuest = trackedQuest == null ? Optional.empty() : trackedQuest;
        unlockedTitles = unlockedTitles == null ? Set.of() : Set.copyOf(unlockedTitles);
        achievements = achievements == null ? Set.of() : Set.copyOf(achievements);
        if (updatedAt < 1) {
            updatedAt = System.currentTimeMillis();
        }
    }

    public static PlayerQuestData empty(UUID playerId) {
        return new PlayerQuestData(playerId, Map.of(), Set.of(), Optional.empty(), 0, 0, Set.of(), Set.of(), System.currentTimeMillis());
    }

    public boolean isActive(QuestId questId) {
        return activeQuests.containsKey(questId);
    }

    public boolean isCompleted(QuestId questId) {
        return completedQuests.contains(questId);
    }

    public PlayerQuestData withActiveQuest(QuestId questId) {
        Map<QuestId, PlayerQuestProgress> active = new LinkedHashMap<>(activeQuests);
        active.put(questId, PlayerQuestProgress.started(questId));
        return new PlayerQuestData(playerId, active, completedQuests, trackedQuest, questPoints, reputation, unlockedTitles, achievements, System.currentTimeMillis());
    }

    public PlayerQuestData withoutActiveQuest(QuestId questId) {
        Map<QuestId, PlayerQuestProgress> active = new LinkedHashMap<>(activeQuests);
        active.remove(questId);
        Optional<QuestId> tracked = trackedQuest.filter(current -> !current.equals(questId));
        return new PlayerQuestData(playerId, active, completedQuests, tracked, questPoints, reputation, unlockedTitles, achievements, System.currentTimeMillis());
    }

    public PlayerQuestData withCompletedQuest(QuestId questId) {
        Map<QuestId, PlayerQuestProgress> active = new LinkedHashMap<>(activeQuests);
        active.remove(questId);
        java.util.LinkedHashSet<QuestId> completed = new java.util.LinkedHashSet<>(completedQuests);
        completed.add(questId);
        Optional<QuestId> tracked = trackedQuest.filter(current -> !current.equals(questId));
        return new PlayerQuestData(playerId, active, completed, tracked, questPoints, reputation, unlockedTitles, achievements, System.currentTimeMillis());
    }

    public PlayerQuestData withTrackedQuest(Optional<QuestId> questId) {
        return new PlayerQuestData(playerId, activeQuests, completedQuests, questId, questPoints, reputation, unlockedTitles, achievements, System.currentTimeMillis());
    }

    public Optional<PlayerQuestProgress> progress(QuestId questId) {
        return Optional.ofNullable(activeQuests.get(questId));
    }

    public PlayerQuestData withProgress(PlayerQuestProgress progress) {
        Map<QuestId, PlayerQuestProgress> active = new LinkedHashMap<>(activeQuests);
        active.put(progress.questId(), progress);
        return new PlayerQuestData(playerId, active, completedQuests, trackedQuest, questPoints, reputation, unlockedTitles, achievements, System.currentTimeMillis());
    }

    public PlayerQuestData addQuestPoints(int amount) {
        return new PlayerQuestData(playerId, activeQuests, completedQuests, trackedQuest, Math.max(0, questPoints + amount), reputation, unlockedTitles, achievements, System.currentTimeMillis());
    }

    public PlayerQuestData addReputation(int amount) {
        return new PlayerQuestData(playerId, activeQuests, completedQuests, trackedQuest, questPoints, Math.max(0, reputation + amount), unlockedTitles, achievements, System.currentTimeMillis());
    }

    public PlayerQuestData unlockTitle(String title) {
        java.util.LinkedHashSet<String> titles = new java.util.LinkedHashSet<>(unlockedTitles);
        if (title != null && !title.isBlank()) {
            titles.add(title.trim());
        }
        return new PlayerQuestData(playerId, activeQuests, completedQuests, trackedQuest, questPoints, reputation, titles, achievements, System.currentTimeMillis());
    }

    public PlayerQuestData unlockAchievement(String achievement) {
        java.util.LinkedHashSet<String> updatedAchievements = new java.util.LinkedHashSet<>(achievements);
        if (achievement != null && !achievement.isBlank()) {
            updatedAchievements.add(achievement.trim());
        }
        return new PlayerQuestData(playerId, activeQuests, completedQuests, trackedQuest, questPoints, reputation, unlockedTitles, updatedAchievements, System.currentTimeMillis());
    }
}
