package de.craftplay.quests.storage;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface StorageProvider {

    String id();

    CompletableFuture<Void> initialize();

    CompletableFuture<Void> shutdown();

    CompletableFuture<Optional<String>> loadDocument(StorageDocumentKey key);

    CompletableFuture<Set<String>> listDocuments(String namespace);

    CompletableFuture<Void> saveDocument(StorageDocumentKey key, String content);

    CompletableFuture<Boolean> deleteDocument(StorageDocumentKey key);
}
