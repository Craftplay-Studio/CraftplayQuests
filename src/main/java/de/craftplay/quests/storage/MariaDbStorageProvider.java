package de.craftplay.quests.storage;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.library.LibraryLoaderService;
import de.craftplay.quests.scheduler.AsyncTaskService;

public final class MariaDbStorageProvider extends SqlStorageProvider {

    public MariaDbStorageProvider(
        CraftplayQuestsPlugin plugin,
        AsyncTaskService asyncTaskService,
        LibraryLoaderService libraryLoaderService
    ) {
        super(plugin, asyncTaskService, libraryLoaderService);
    }

    @Override
    public String id() {
        return "MARIADB";
    }

    @Override
    protected String driverKey() {
        return "mariadb";
    }

    @Override
    protected String jdbcUrl() {
        String host = plugin().getConfig().getString("storage.mariadb.host", "localhost");
        int port = plugin().getConfig().getInt("storage.mariadb.port", 3306);
        String database = plugin().getConfig().getString("storage.mariadb.database", "craftplay_quests");
        return "jdbc:mariadb://" + host + ":" + port + "/" + database
            + "?useUnicode=true&characterEncoding=utf8";
    }

    @Override
    protected String username() {
        return plugin().getConfig().getString("storage.mariadb.username", "root");
    }

    @Override
    protected String password() {
        return plugin().getConfig().getString("storage.mariadb.password", "");
    }
}
