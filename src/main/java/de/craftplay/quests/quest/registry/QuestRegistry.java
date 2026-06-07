package de.craftplay.quests.quest.registry;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.storage.QuestSerializer;
import de.craftplay.quests.storage.StorageDocumentKey;
import de.craftplay.quests.storage.StorageService;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class QuestRegistry {

    private static final String QUEST_NAMESPACE = "quests";

    private final CraftplayQuestsPlugin plugin;
    private final StorageService storageService;
    private final QuestSerializer serializer = new QuestSerializer();
    private final Map<QuestId, Quest> quests = new ConcurrentHashMap<>();

    public QuestRegistry(CraftplayQuestsPlugin plugin, StorageService storageService) {
        this.plugin = plugin;
        this.storageService = storageService;
    }

    public CompletableFuture<Void> loadAll() {
        return storageService.listDocuments(QUEST_NAMESPACE).thenCompose(keys -> {
            List<CompletableFuture<Optional<Quest>>> futures = keys.stream()
                .map(this::loadQuestDocument)
                .toList();

            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
                Map<QuestId, Quest> loaded = new ConcurrentHashMap<>();
                for (CompletableFuture<Optional<Quest>> future : futures) {
                    future.join().ifPresent(quest -> loaded.put(quest.id(), quest));
                }
                quests.clear();
                quests.putAll(loaded);
                plugin.getLogger().info("Loaded " + loaded.size() + " quests.");
            });
        });
    }

    public CompletableFuture<Void> save(Quest quest) {
        return storageService.saveDocument(
            new StorageDocumentKey(QUEST_NAMESPACE, quest.id().value()),
            serializer.serialize(quest)
        ).thenRun(() -> quests.put(quest.id(), quest));
    }

    public CompletableFuture<Boolean> delete(QuestId questId) {
        return storageService.deleteDocument(new StorageDocumentKey(QUEST_NAMESPACE, questId.value()))
            .thenApply(deleted -> {
                quests.remove(questId);
                return deleted;
            });
    }

    public Optional<Quest> find(QuestId questId) {
        return Optional.ofNullable(quests.get(questId));
    }

    public Collection<Quest> all() {
        return List.copyOf(quests.values());
    }

    public List<Quest> sortedById() {
        return quests.values().stream()
            .sorted(Comparator.comparing(quest -> quest.id().value()))
            .toList();
    }

    public int size() {
        return quests.size();
    }

    private CompletableFuture<Optional<Quest>> loadQuestDocument(String key) {
        return storageService.loadDocument(new StorageDocumentKey(QUEST_NAMESPACE, key))
            .thenApply(document -> document.map(serializer::deserialize))
            .handle((quest, throwable) -> {
                if (throwable == null) {
                    return quest;
                }
                plugin.getLogger().warning("Could not load quest document " + key + ": " + throwable.getMessage());
                return Optional.empty();
            });
    }
}
