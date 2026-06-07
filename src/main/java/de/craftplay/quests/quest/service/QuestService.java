package de.craftplay.quests.quest.service;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.player.PlayerQuestData;
import de.craftplay.quests.quest.player.PlayerQuestDataRepository;
import de.craftplay.quests.quest.registry.QuestRegistry;
import de.craftplay.quests.storage.StorageService;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class QuestService implements QuestApi {

    private final QuestRegistry questRegistry;
    private final PlayerQuestDataRepository playerQuestDataRepository;

    public QuestService(CraftplayQuestsPlugin plugin, StorageService storageService) {
        this.questRegistry = new QuestRegistry(plugin, storageService);
        this.playerQuestDataRepository = new PlayerQuestDataRepository(storageService);
    }

    public CompletableFuture<Void> initialize() {
        return questRegistry.loadAll();
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
    public CompletableFuture<PlayerQuestData> playerData(UUID playerId) {
        return playerQuestDataRepository.load(playerId);
    }

    @Override
    public CompletableFuture<PlayerQuestData> acceptQuest(UUID playerId, QuestId questId) {
        Optional<Quest> optionalQuest = questRegistry.find(questId);
        if (optionalQuest.isEmpty()) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Quest not found: " + questId.value()));
        }

        Quest quest = optionalQuest.get();
        if (!quest.enabled()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Quest is disabled: " + questId.value()));
        }

        return playerQuestDataRepository.load(playerId).thenCompose(data -> {
            if (data.isActive(questId)) {
                throw new IllegalStateException("Quest is already active: " + questId.value());
            }
            if (data.isCompleted(questId)) {
                throw new IllegalStateException("Quest is already completed: " + questId.value());
            }
            if (!data.completedQuests().containsAll(quest.requiresCompleted())) {
                throw new IllegalStateException("Quest requirements are not met: " + questId.value());
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

            PlayerQuestData updated = data.withCompletedQuest(questId);
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
