package de.craftplay.quests.quest.player;

import de.craftplay.quests.quest.storage.PlayerQuestDataSerializer;
import de.craftplay.quests.storage.StorageDocumentKey;
import de.craftplay.quests.storage.StorageService;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerQuestDataRepository {

    private static final String PLAYERDATA_NAMESPACE = "playerdata";

    private final StorageService storageService;
    private final PlayerQuestDataSerializer serializer = new PlayerQuestDataSerializer();
    private final Map<UUID, PlayerQuestData> cache = new ConcurrentHashMap<>();

    public PlayerQuestDataRepository(StorageService storageService) {
        this.storageService = storageService;
    }

    public CompletableFuture<PlayerQuestData> load(UUID playerId) {
        PlayerQuestData cached = cache.get(playerId);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return storageService.loadDocument(new StorageDocumentKey(PLAYERDATA_NAMESPACE, playerId.toString()))
            .thenApply(document -> document
                .map(content -> serializer.deserialize(playerId, content))
                .orElseGet(() -> PlayerQuestData.empty(playerId)))
            .thenApply(data -> {
                cache.put(playerId, data);
                return data;
            });
    }

    public CompletableFuture<List<PlayerQuestData>> loadAll() {
        return storageService.listDocuments(PLAYERDATA_NAMESPACE)
            .thenCompose(keys -> {
                List<CompletableFuture<PlayerQuestData>> futures = keys.stream()
                    .map(this::loadSafely)
                    .toList();
                return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                    .thenApply(ignored -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
            });
    }

    public CompletableFuture<Void> save(PlayerQuestData data) {
        return storageService.saveDocument(
            new StorageDocumentKey(PLAYERDATA_NAMESPACE, data.playerId().toString()),
            serializer.serialize(data)
        ).thenRun(() -> cache.put(data.playerId(), data));
    }

    public Optional<PlayerQuestData> cached(UUID playerId) {
        return Optional.ofNullable(cache.get(playerId));
    }

    public Collection<PlayerQuestData> cachedData() {
        return List.copyOf(cache.values());
    }

    private CompletableFuture<PlayerQuestData> loadSafely(String key) {
        try {
            return load(UUID.fromString(key));
        } catch (IllegalArgumentException exception) {
            return CompletableFuture.completedFuture(PlayerQuestData.empty(new UUID(0L, 0L)));
        }
    }

    public void invalidate(UUID playerId) {
        cache.remove(playerId);
    }

    public void clearCache() {
        cache.clear();
    }
}
