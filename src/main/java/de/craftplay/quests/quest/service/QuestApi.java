package de.craftplay.quests.quest.service;

import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.model.ObjectiveType;
import de.craftplay.quests.quest.objective.ObjectiveProgressResult;
import de.craftplay.quests.quest.player.PlayerQuestData;
import de.craftplay.quests.quest.registry.QuestRegistry;
import de.craftplay.quests.quest.requirement.RequirementCheckResult;
import de.craftplay.quests.quest.reward.RewardPlan;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface QuestApi {

    QuestRegistry registry();

    CompletableFuture<Void> saveQuest(Quest quest);

    CompletableFuture<Boolean> deleteQuest(QuestId questId);

    Optional<Quest> findQuest(QuestId questId);

    Collection<Quest> quests();

    RequirementCheckResult canAccept(PlayerQuestData data, Quest quest);

    RewardPlan rewardPlan(QuestId questId);

    CompletableFuture<PlayerQuestData> playerData(UUID playerId);

    CompletableFuture<ObjectiveProgressResult> recordObjectiveProgress(
        UUID playerId,
        ObjectiveType type,
        String target,
        int amount
    );

    CompletableFuture<PlayerQuestData> acceptQuest(UUID playerId, QuestId questId);

    CompletableFuture<PlayerQuestData> completeQuest(UUID playerId, QuestId questId);

    CompletableFuture<PlayerQuestData> cancelQuest(UUID playerId, QuestId questId);

    CompletableFuture<PlayerQuestData> trackQuest(UUID playerId, QuestId questId);

    CompletableFuture<PlayerQuestData> untrackQuest(UUID playerId);
}
