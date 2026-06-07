package de.craftplay.quests.storage;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.library.LibraryLoaderService;
import de.craftplay.quests.scheduler.AsyncTaskService;

public final class MySqlStorageProvider extends SqlStorageProvider {

    public MySqlStorageProvider(
        CraftplayQuestsPlugin plugin,
        AsyncTaskService asyncTaskService,
        LibraryLoaderService libraryLoaderService
    ) {
        super(plugin, asyncTaskService, libraryLoaderService);
    }

    @Override
    public String id() {
        return "MYSQL";
    }

    @Override
    protected String driverKey() {
        return "mysql";
    }

    @Override
    protected String jdbcUrl() {
        String host = plugin().getConfig().getString("storage.mysql.host", "localhost");
        int port = plugin().getConfig().getInt("storage.mysql.port", 3306);
        String database = plugin().getConfig().getString("storage.mysql.database", "craftplay_quests");
        return "jdbc:mysql://" + host + ":" + port + "/" + database
            + "?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true";
    }

    @Override
    protected String username() {
        return plugin().getConfig().getString("storage.mysql.username", "root");
    }

    @Override
    protected String password() {
        return plugin().getConfig().getString("storage.mysql.password", "");
    }
}
