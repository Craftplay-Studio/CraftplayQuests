package de.craftplay.quests.config;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.security.TokenGenerator;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigBootstrapService {

    private static final List<String> DIRECTORIES = List.of(
        "gui",
        "language",
        "save/h2",
        "save/sqlite",
        "save/yaml/quests",
        "save/yaml/npcs",
        "save/yaml/playerdata",
        "save/yaml/progress",
        "save/yaml/titles",
        "save/yaml/factions",
        "save/yaml/achievements",
        "save/cache/skins/npc_skins",
        "save/cache/skins/player_skins",
        "save/cache/heads/headdatabase",
        "save/cache/heads/custom_heads",
        "save/cache/statistics",
        "save/cache/leaderboards",
        "save/backups",
        "save/exports",
        "skins",
        "import",
        "lib",
        "logs",
        "temp"
    );

    private static final List<String> RESOURCE_FILES = List.of(
        "gui/main.yml",
        "gui/quest_categories.yml",
        "gui/quest_details.yml",
        "gui/player_quests.yml",
        "gui/adventure_book.yml",
        "gui/achievements.yml",
        "gui/admin_main.yml",
        "gui/admin_quests.yml",
        "gui/admin_npcs.yml",
        "gui/admin_categories.yml",
        "gui/admin_titles.yml",
        "gui/admin_settings.yml",
        "language/de_DE.yml",
        "language/en_US.yml",
        "language/fr_FR.yml",
        "language/es_ES.yml",
        "language/it_IT.yml",
        "language/nl_NL.yml",
        "language/pl_PL.yml",
        "language/tr_TR.yml",
        "language/ru_RU.yml",
        "language/pt_BR.yml"
    );

    private final CraftplayQuestsPlugin plugin;

    public ConfigBootstrapService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void bootstrap() throws IOException {
        Files.createDirectories(plugin.getDataFolder().toPath());
        createDirectories();
        saveResourceIfMissing("config.yml");
        createServerConfiguration();
        createMetadataFiles();

        for (String resource : RESOURCE_FILES) {
            saveResourceIfMissing(resource);
        }
    }

    private void createDirectories() throws IOException {
        Path dataFolder = plugin.getDataFolder().toPath();
        for (String directory : DIRECTORIES) {
            Files.createDirectories(dataFolder.resolve(directory));
        }
    }

    private void createServerConfiguration() throws IOException {
        File serverFile = new File(plugin.getDataFolder(), "server.yml");
        YamlConfiguration server = YamlConfiguration.loadConfiguration(serverFile);
        boolean changed = false;

        changed |= setDefault(server, "server.id", "citybuild");
        changed |= setDefault(server, "server.display-name", "Craftplay CityBuild");
        changed |= setDefault(server, "security.install-token", TokenGenerator.installToken());
        changed |= setDefault(server, "security.recovery-code", TokenGenerator.recoveryCode());
        changed |= setDefault(server, "api.tokens.panel", TokenGenerator.apiToken("panel"));
        changed |= setDefault(server, "api.tokens.homepage", TokenGenerator.apiToken("homepage"));

        if (!serverFile.exists() || changed) {
            server.save(serverFile);
        }
    }

    private void createMetadataFiles() throws IOException {
        createEmptyYamlIfMissing("save/cache/skins/metadata.yml");
        createEmptyYamlIfMissing("save/cache/heads/metadata.yml");
    }

    private void createEmptyYamlIfMissing(String relativePath) throws IOException {
        Path path = plugin.getDataFolder().toPath().resolve(relativePath);
        if (Files.notExists(path)) {
            Files.createDirectories(path.getParent());
            Files.writeString(path, "# Managed by CraftplayQuests.\n");
        }
    }

    private void saveResourceIfMissing(String resourcePath) {
        File target = new File(plugin.getDataFolder(), resourcePath);
        if (!target.exists()) {
            plugin.saveResource(resourcePath, false);
        }
    }

    private boolean setDefault(YamlConfiguration configuration, String path, Object value) {
        if (configuration.contains(path)) {
            return false;
        }

        configuration.set(path, value);
        return true;
    }
}
