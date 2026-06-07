package de.craftplay.quests.storage;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {

    String id();

    CompletableFuture<Void> initialize();

    CompletableFuture<Void> shutdown();

    CompletableFuture<Optional<String>> loadDocument(StorageDocumentKey key);

    CompletableFuture<Void> saveDocument(StorageDocumentKey key, String content);

    CompletableFuture<Boolean> deleteDocument(StorageDocumentKey key);
}
