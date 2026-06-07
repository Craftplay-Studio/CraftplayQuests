package de.craftplay.quests.config;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ServerSettingsService {

    private final CraftplayQuestsPlugin plugin;
    private YamlConfiguration serverConfiguration;

    public ServerSettingsService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        File serverFile = new File(plugin.getDataFolder(), "server.yml");
        this.serverConfiguration = YamlConfiguration.loadConfiguration(serverFile);
    }

    public String serverId() {
        return serverConfiguration.getString("server.id", "unknown");
    }

    public String displayName() {
        return serverConfiguration.getString("server.display-name", serverId());
    }
}
