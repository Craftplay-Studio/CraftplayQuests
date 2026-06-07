package de.craftplay.quests.storage;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.library.LibraryLoaderService;
import de.craftplay.quests.scheduler.AsyncTaskService;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

abstract class SqlStorageProvider implements StorageProvider {

    private final CraftplayQuestsPlugin plugin;
    private final AsyncTaskService asyncTaskService;
    private final LibraryLoaderService libraryLoaderService;

    SqlStorageProvider(
        CraftplayQuestsPlugin plugin,
        AsyncTaskService asyncTaskService,
        LibraryLoaderService libraryLoaderService
    ) {
        this.plugin = plugin;
        this.asyncTaskService = asyncTaskService;
        this.libraryLoaderService = libraryLoaderService;
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return asyncTaskService.runAsync(() -> {
            libraryLoaderService.registerJdbcDriver(driverKey());
            try (Connection connection = connection();
                 Statement statement = connection.createStatement()) {
                statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS cpq_documents (
                      namespace VARCHAR(64) NOT NULL,
                      doc_key VARCHAR(128) NOT NULL,
                      content LONGTEXT NOT NULL,
                      updated_at BIGINT NOT NULL,
                      PRIMARY KEY (namespace, doc_key)
                    ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                    """);
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not initialize " + id() + " storage", exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Optional<String>> loadDocument(StorageDocumentKey key) {
        return asyncTaskService.supplyAsync(() -> {
            try (Connection connection = connection();
                 PreparedStatement statement = connection.prepareStatement("""
                     SELECT content FROM cpq_documents WHERE namespace = ? AND doc_key = ?
                     """)) {
                statement.setString(1, key.namespace());
                statement.setString(2, key.key());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return Optional.empty();
                    }
                    return Optional.of(resultSet.getString("content"));
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not load " + id() + " document " + key, exception);
            }
        });
    }

    @Override
    public CompletableFuture<Set<String>> listDocuments(String namespace) {
        return asyncTaskService.supplyAsync(() -> {
            try (Connection connection = connection();
                 PreparedStatement statement = connection.prepareStatement("""
                     SELECT doc_key FROM cpq_documents WHERE namespace = ?
                     """)) {
                statement.setString(1, new StorageDocumentKey(namespace, "validation").namespace());
                try (ResultSet resultSet = statement.executeQuery()) {
                    java.util.ArrayList<String> keys = new java.util.ArrayList<>();
                    while (resultSet.next()) {
                        keys.add(resultSet.getString("doc_key"));
                    }
                    return keys.stream().collect(Collectors.toUnmodifiableSet());
                }
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not list " + id() + " namespace " + namespace, exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveDocument(StorageDocumentKey key, String content) {
        return asyncTaskService.runAsync(() -> saveDocumentBlocking(key, content));
    }

    void saveDocumentBlocking(StorageDocumentKey key, String content) {
        try (Connection connection = connection();
             PreparedStatement statement = connection.prepareStatement("""
                 INSERT INTO cpq_documents (namespace, doc_key, content, updated_at)
                 VALUES (?, ?, ?, ?)
                 ON DUPLICATE KEY UPDATE content = VALUES(content), updated_at = VALUES(updated_at)
                 """)) {
            statement.setString(1, key.namespace());
            statement.setString(2, key.key());
            statement.setString(3, content);
            statement.setLong(4, System.currentTimeMillis());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save " + id() + " document " + key, exception);
        }
    }

    @Override
    public CompletableFuture<Boolean> deleteDocument(StorageDocumentKey key) {
        return asyncTaskService.supplyAsync(() -> {
            try (Connection connection = connection();
                 PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM cpq_documents WHERE namespace = ? AND doc_key = ?
                     """)) {
                statement.setString(1, key.namespace());
                statement.setString(2, key.key());
                return statement.executeUpdate() > 0;
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not delete " + id() + " document " + key, exception);
            }
        });
    }

    protected CraftplayQuestsPlugin plugin() {
        return plugin;
    }

    protected Connection connection() throws SQLException {
        return DriverManager.getConnection(jdbcUrl(), username(), password());
    }

    protected abstract String driverKey();

    protected abstract String jdbcUrl();

    protected abstract String username();

    protected abstract String password();
}
