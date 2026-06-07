package de.craftplay.quests;

import de.craftplay.quests.command.QuestsCommand;
import de.craftplay.quests.config.ConfigBootstrapService;
import de.craftplay.quests.config.ServerSettingsService;
import de.craftplay.quests.language.LanguageService;
import java.io.IOException;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class CraftplayQuestsPlugin extends JavaPlugin {

    private ConfigBootstrapService configBootstrapService;
    private ServerSettingsService serverSettingsService;
    private LanguageService languageService;

    @Override
    public void onEnable() {
        this.configBootstrapService = new ConfigBootstrapService(this);

        try {
            configBootstrapService.bootstrap();
        } catch (IOException exception) {
            getLogger().severe("CraftplayQuests could not create its required files: " + exception.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        reloadConfig();
        this.serverSettingsService = new ServerSettingsService(this);
        this.serverSettingsService.reload();
        this.languageService = new LanguageService(this);
        this.languageService.reload();

        registerCommands();

        getLogger().info("CraftplayQuests enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("CraftplayQuests disabled.");
    }

    public void reloadPluginConfiguration() {
        reloadConfig();
        if (serverSettingsService != null) {
            serverSettingsService.reload();
        }
        if (languageService != null) {
            languageService.reload();
        }
    }

    public LanguageService language() {
        return languageService;
    }

    public ServerSettingsService serverSettings() {
        return serverSettingsService;
    }

    private void registerCommands() {
        PluginCommand questsCommand = getCommand("quests");
        if (questsCommand == null) {
            getLogger().severe("Command /quests is missing from plugin.yml.");
            return;
        }

        QuestsCommand executor = new QuestsCommand(this);
        questsCommand.setExecutor(executor);
        questsCommand.setTabCompleter(executor);
    }
}
