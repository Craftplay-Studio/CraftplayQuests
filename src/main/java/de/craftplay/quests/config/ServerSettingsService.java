package de.craftplay.quests.config;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.io.File;
import java.util.Optional;
import org.bukkit.configuration.ConfigurationSection;
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

    public Optional<String> apiToken(String name) {
        String token = serverConfiguration.getString("api.tokens." + name, "");
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(token);
    }

    public boolean hasApiToken(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        ConfigurationSection section = serverConfiguration.getConfigurationSection("api.tokens");
        if (section == null) {
            return false;
        }
        for (String key : section.getKeys(false)) {
            if (token.equals(section.getString(key))) {
                return true;
            }
        }
        return false;
    }
}
