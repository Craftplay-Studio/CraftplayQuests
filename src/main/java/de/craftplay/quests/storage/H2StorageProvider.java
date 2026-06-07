package de.craftplay.quests.storage;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.library.LibraryLoaderService;
import de.craftplay.quests.scheduler.AsyncTaskService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class H2StorageProvider implements StorageProvider {

    private final CraftplayQuestsPlugin plugin;
    private final AsyncTaskService asyncTaskService;
    private final LibraryLoaderService libraryLoaderService;

    public H2StorageProvider(
        CraftplayQuestsPlugin plugin,
        AsyncTaskService asyncTaskService,
        LibraryLoaderService libraryLoaderService
    ) {
        this.plugin = plugin;
        this.asyncTaskService = asyncTaskService;
        this.libraryLoaderService = libraryLoaderService;
    }

    @Override
    public String id() {
        return "H2";
    }

    @Override
    public CompletableFuture<Void> initialize() {
        return asyncTaskService.runAsync(() -> {
            try {
                Files.createDirectories(databaseFile().getParent());
                libraryLoaderService.registerJdbcDriver("h2");
                try (Connection connection = connection();
                     Statement statement = connection.createStatement()) {
                    statement.executeUpdate("""
                        CREATE TABLE IF NOT EXISTS cpq_documents (
                          namespace VARCHAR(64) NOT NULL,
                          doc_key VARCHAR(128) NOT NULL,
                          content CLOB NOT NULL,
                          updated_at BIGINT NOT NULL,
                          PRIMARY KEY (namespace, doc_key)
                        )
                        """);
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Could not initialize H2 storage", exception);
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
                throw new IllegalStateException("Could not load H2 document " + key, exception);
            }
        });
    }

    @Override
    public CompletableFuture<Void> saveDocument(StorageDocumentKey key, String content) {
        return asyncTaskService.runAsync(() -> {
            try (Connection connection = connection();
                 PreparedStatement statement = connection.prepareStatement("""
                     MERGE INTO cpq_documents (namespace, doc_key, content, updated_at)
                     KEY(namespace, doc_key)
                     VALUES (?, ?, ?, ?)
                     """)) {
                statement.setString(1, key.namespace());
                statement.setString(2, key.key());
                statement.setString(3, content);
                statement.setLong(4, System.currentTimeMillis());
                statement.executeUpdate();
            } catch (SQLException exception) {
                throw new IllegalStateException("Could not save H2 document " + key, exception);
            }
        });
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
                throw new IllegalStateException("Could not delete H2 document " + key, exception);
            }
        });
    }

    private Connection connection() throws SQLException {
        String url = "jdbc:h2:" + databaseFile().toString().replace('\\', '/') + ";MODE=MySQL;DATABASE_TO_LOWER=TRUE";
        return DriverManager.getConnection(url, "sa", "");
    }

    private Path databaseFile() {
        String configured = plugin.getConfig().getString("storage.h2.file", "save/h2/quests");
        Path dataFolder = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path file = dataFolder.resolve(configured).normalize();
        if (!file.startsWith(dataFolder)) {
            throw new IllegalArgumentException("H2 storage file must stay inside the plugin data folder.");
        }
        return file;
    }
}
