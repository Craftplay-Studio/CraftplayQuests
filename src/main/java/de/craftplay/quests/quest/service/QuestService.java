package de.craftplay.quests.quest.service;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.achievement.AchievementService;
import de.craftplay.quests.quest.model.ObjectiveType;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.objective.ObjectiveProgressResult;
import de.craftplay.quests.quest.objective.ObjectiveService;
import de.craftplay.quests.quest.player.PlayerQuestData;
import de.craftplay.quests.quest.player.PlayerQuestDataRepository;
import de.craftplay.quests.quest.registry.QuestRegistry;
import de.craftplay.quests.quest.requirement.RequirementCheckResult;
import de.craftplay.quests.quest.requirement.RequirementService;
import de.craftplay.quests.quest.reward.RewardPlan;
import de.craftplay.quests.quest.reward.RewardService;
import de.craftplay.quests.quest.seed.QuestSeedService;
import de.craftplay.quests.storage.StorageService;
import de.craftplay.quests.scheduler.MainThreadService;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class QuestService implements QuestApi {

    private final QuestRegistry questRegistry;
    private final PlayerQuestDataRepository playerQuestDataRepository;
    private final ObjectiveService objectiveService;
    private final RequirementService requirementService;
    private final RewardService rewardService;
    private final AchievementService achievementService;
    private final QuestSeedService questSeedService;

    public QuestService(
        CraftplayQuestsPlugin plugin,
        StorageService storageService,
        MainThreadService mainThreadService,
        AchievementService achievementService
    ) {
        this.questRegistry = new QuestRegistry(plugin, storageService);
        this.playerQuestDataRepository = new PlayerQuestDataRepository(storageService);
        this.objectiveService = new ObjectiveService();
        this.requirementService = new RequirementService();
        this.rewardService = new RewardService(plugin, mainThreadService);
        this.achievementService = achievementService;
        this.questSeedService = new QuestSeedService();
    }

    public CompletableFuture<Void> initialize() {
        return questRegistry.loadAll()
            .thenCompose(ignored -> questSeedService.seedDefaults(questRegistry));
    }

    public CompletableFuture<Void> shutdown() {
        playerQuestDataRepository.clearCache();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public QuestRegistry registry() {
        return questRegistry;
    }

    @Override
    public CompletableFuture<Void> saveQuest(Quest quest) {
        return questRegistry.save(quest);
    }

    @Override
    public CompletableFuture<Boolean> deleteQuest(QuestId questId) {
        return questRegistry.delete(questId);
    }

    @Override
    public Optional<Quest> findQuest(QuestId questId) {
        return questRegistry.find(questId);
    }

    @Override
    public Collection<Quest> quests() {
        return questRegistry.all();
    }

    @Override
    public RequirementCheckResult canAccept(PlayerQuestData data, Quest quest) {
        return requirementService.checkAccept(quest, data);
    }

    @Override
    public RewardPlan rewardPlan(QuestId questId) {
        Quest quest = questRegistry.find(questId)
            .orElseThrow(() -> new IllegalArgumentException("Quest not found: " + questId.value()));
        return rewardService.planRewards(quest);
    }

    @Override
    public CompletableFuture<PlayerQuestData> playerData(UUID playerId) {
        return playerQuestDataRepository.load(playerId);
    }

    @Override
    public Optional<PlayerQuestData> cachedPlayerData(UUID playerId) {
        return playerQuestDataRepository.cached(playerId);
    }

    @Override
    public CompletableFuture<ObjectiveProgressResult> recordObjectiveProgress(
        UUID playerId,
        ObjectiveType type,
        String target,
        int amount
    ) {
        return playerQuestDataRepository.load(playerId).thenCompose(data -> {
            Collection<Quest> activeQuests = data.activeQuests().keySet().stream()
                .map(questRegistry::find)
                .flatMap(Optional::stream)
                .toList();

            ObjectiveProgressResult result = objectiveService.applyProgress(data, activeQuests, type, target, amount);
            if (!result.changed()) {
                return CompletableFuture.completedFuture(result);
            }

            return playerQuestDataRepository.save(result.playerData()).thenApply(ignored -> result);
        });
    }

    @Override
    public CompletableFuture<PlayerQuestData> acceptQuest(UUID playerId, QuestId questId) {
        Optional<Quest> optionalQuest = questRegistry.find(questId);
        if (optionalQuest.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Quest not found: " + questId.value()));
        }

        Quest quest = optionalQuest.get();
        return playerQuestDataRepository.load(playerId).thenCompose(data -> {
            RequirementCheckResult requirementResult = requirementService.checkAccept(quest, data);
            if (!requirementResult.allowed()) {
                throw new IllegalStateException("Quest requirements are not met: " + String.join(",", requirementResult.missingReasons()));
            }

            PlayerQuestData updated = data.withActiveQuest(questId);
            return playerQuestDataRepository.save(updated).thenApply(ignored -> updated);
        });
    }

    @Override
    public CompletableFuture<PlayerQuestData> completeQuest(UUID playerId, QuestId questId) {
        return playerQuestDataRepository.load(playerId).thenCompose(data -> {
            if (!data.isActive(questId)) {
                throw new IllegalStateException("Quest is not active: " + questId.value());
            }
            Quest quest = questRegistry.find(questId)
                .orElseThrow(() -> new IllegalArgumentException("Quest not found: " + questId.value()));
            if (!objectiveService.isQuestComplete(quest, data)) {
                throw new IllegalStateException("Quest objectives are not complete: " + questId.value());
            }

            boolean firstCompletedQuest = data.completedQuests().isEmpty();
            PlayerQuestData completed = data.withCompletedQuest(questId);
            PlayerQuestData withAchievements = achievementService.applyQuestCompletionAchievements(
                completed,
                questId,
                firstCompletedQuest
            );
            PlayerQuestData updated = rewardService.applyRewards(withAchievements, quest);
            return playerQuestDataRepository.save(updated).thenApply(ignored -> updated);
        });
    }

    @Override
    public CompletableFuture<PlayerQuestData> cancelQuest(UUID playerId, QuestId questId) {
        return playerQuestDataRepository.load(playerId).thenCompose(data -> {
            if (!data.isActive(questId)) {
                return CompletableFuture.completedFuture(data);
            }

            PlayerQuestData updated = data.withoutActiveQuest(questId);
            return playerQuestDataRepository.save(updated).thenApply(ignored -> updated);
        });
    }

    @Override
    public CompletableFuture<PlayerQuestData> trackQuest(UUID playerId, QuestId questId) {
        return playerQuestDataRepository.load(playerId).thenCompose(data -> {
            if (!data.isActive(questId)) {
                throw new IllegalStateException("Quest is not active: " + questId.value());
            }

            PlayerQuestData updated = data.withTrackedQuest(Optional.of(questId));
            return playerQuestDataRepository.save(updated).thenApply(ignored -> updated);
        });
    }

    @Override
    public CompletableFuture<PlayerQuestData> untrackQuest(UUID playerId) {
        return playerQuestDataRepository.load(playerId).thenCompose(data -> {
            PlayerQuestData updated = data.withTrackedQuest(Optional.empty());
            return playerQuestDataRepository.save(updated).thenApply(ignored -> updated);
        });
    }
}
