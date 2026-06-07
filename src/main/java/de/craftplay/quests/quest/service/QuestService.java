package de.craftplay.quests.quest.service;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.achievement.AchievementService;
import de.craftplay.quests.quest.model.ObjectiveType;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.model.QuestRequirement;
import de.craftplay.quests.quest.model.RequirementType;
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
import de.craftplay.quests.reset.ResetService;
import de.craftplay.quests.storage.StorageService;
import de.craftplay.quests.scheduler.MainThreadService;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class QuestService implements QuestApi {

    private final CraftplayQuestsPlugin plugin;
    private final QuestRegistry questRegistry;
    private final PlayerQuestDataRepository playerQuestDataRepository;
    private final ObjectiveService objectiveService;
    private final RequirementService requirementService;
    private final RewardService rewardService;
    private final AchievementService achievementService;
    private final MainThreadService mainThreadService;
    private final QuestSeedService questSeedService;

    public QuestService(
        CraftplayQuestsPlugin plugin,
        StorageService storageService,
        MainThreadService mainThreadService,
        AchievementService achievementService
    ) {
        this.plugin = plugin;
        this.questRegistry = new QuestRegistry(plugin, storageService);
        this.playerQuestDataRepository = new PlayerQuestDataRepository(storageService);
        this.objectiveService = new ObjectiveService();
        this.requirementService = new RequirementService();
        this.rewardService = new RewardService(plugin, mainThreadService);
        this.achievementService = achievementService;
        this.mainThreadService = mainThreadService;
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
    public CompletableFuture<PlayerQuestData> savePlayerData(PlayerQuestData data) {
        return playerQuestDataRepository.save(data).thenApply(ignored -> data);
    }

    @Override
    public CompletableFuture<List<PlayerQuestData>> allPlayerData() {
        return playerQuestDataRepository.loadAll();
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
            return externalRequirementsAllowed(playerId, quest).thenCompose(externalResult -> {
                if (!externalResult.allowed()) {
                    throw new IllegalStateException("Quest external requirements are not met: " + String.join(",", externalResult.missingReasons()));
                }
                PlayerQuestData updated = data.withActiveQuest(questId);
                return playerQuestDataRepository.save(updated).thenApply(ignored -> updated);
            });
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
    public CompletableFuture<PlayerQuestData> forceCompleteQuest(UUID playerId, QuestId questId) {
        Optional<Quest> optionalQuest = questRegistry.find(questId);
        if (optionalQuest.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Quest not found: " + questId.value()));
        }

        Quest quest = optionalQuest.get();
        return playerQuestDataRepository.load(playerId).thenCompose(data -> {
            boolean firstCompletedQuest = data.completedQuests().isEmpty();
            PlayerQuestData base = data.isActive(questId) ? data : data.withActiveQuest(questId);
            PlayerQuestData completed = base.withCompletedQuest(questId);
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
    public CompletableFuture<PlayerQuestData> resetQuest(UUID playerId, QuestId questId) {
        return playerQuestDataRepository.load(playerId).thenCompose(data -> {
            PlayerQuestData updated = data.withoutActiveQuest(questId).withoutCompletedQuest(questId);
            return playerQuestDataRepository.save(updated).thenApply(ignored -> updated);
        });
    }

    @Override
    public CompletableFuture<PlayerQuestData> cleanupExpiredQuests(UUID playerId) {
        return cleanupExpiredQuestData(playerId).thenApply(ResetService.ResetCleanupResult::data);
    }

    public CompletableFuture<ResetService.ResetCleanupResult> cleanupExpiredQuestData(UUID playerId) {
        return playerQuestDataRepository.load(playerId).thenCompose(data -> {
            var result = plugin.services().resets().cleanupExpiredQuests(data);
            if (result.removedQuests() <= 0) {
                return CompletableFuture.completedFuture(result);
            }
            return playerQuestDataRepository.save(result.data()).thenApply(ignored -> result);
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

    private CompletableFuture<RequirementCheckResult> externalRequirementsAllowed(UUID playerId, Quest quest) {
        return mainThreadService.supply(() -> {
            Player player = Bukkit.getPlayer(playerId);
            java.util.ArrayList<String> missing = new java.util.ArrayList<>();
            for (QuestRequirement requirement : quest.requirements()) {
                if (requirement.type() == RequirementType.PERMISSION && !permissionAllowed(player, requirement)) {
                    missing.add("permission:" + requirement.target());
                }
                if (requirement.type() == RequirementType.ITEM && !itemAllowed(player, requirement)) {
                    missing.add("item:" + requirement.target());
                }
                if (requirement.type() == RequirementType.PLACEHOLDER && !placeholderAllowed(player, requirement)) {
                    missing.add("placeholder:" + requirement.target());
                }
            }
            return missing.isEmpty() ? RequirementCheckResult.success() : RequirementCheckResult.denied(missing);
        });
    }

    private boolean permissionAllowed(Player player, QuestRequirement requirement) {
        if (player == null) {
            return false;
        }
        String permission = requirement.target().isBlank() ? requirement.value() : requirement.target();
        return !permission.isBlank() && player.hasPermission(permission);
    }

    private boolean itemAllowed(Player player, QuestRequirement requirement) {
        if (player == null) {
            return false;
        }
        Material material = Material.matchMaterial(requirement.target());
        if (material == null) {
            return false;
        }
        int amount = integer(requirement.value(), integer(requirement.data().get("amount"), 1));
        return player.getInventory().containsAtLeast(new ItemStack(material), Math.max(1, amount));
    }

    private boolean placeholderAllowed(Player player, QuestRequirement requirement) {
        if (player == null) {
            return false;
        }
        String placeholder = requirement.target().isBlank() ? requirement.data().getOrDefault("placeholder", "") : requirement.target();
        String expected = requirement.value().isBlank() ? requirement.data().getOrDefault("equals", "true") : requirement.value();
        if (placeholder.isBlank()) {
            return false;
        }
        String actual = plugin.services().placeholders().apply(player, placeholder);
        return actual.equalsIgnoreCase(expected) || ("true".equalsIgnoreCase(expected) && "yes".equalsIgnoreCase(actual));
    }

    private int integer(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
