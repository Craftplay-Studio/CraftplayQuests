package de.craftplay.quests.language;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;

public final class LanguageService {

    private static final String DEFAULT_LANGUAGE = "de_DE";

    private final CraftplayQuestsPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private YamlConfiguration language;

    public LanguageService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void reload() {
        String languageName = plugin.getConfig().getString("settings.language", DEFAULT_LANGUAGE);
        File languageFile = new File(plugin.getDataFolder(), "language/" + languageName + ".yml");

        if (!languageFile.exists()) {
            plugin.getLogger().warning("Language file " + languageName + ".yml is missing. Falling back to de_DE.yml.");
            languageFile = new File(plugin.getDataFolder(), "language/" + DEFAULT_LANGUAGE + ".yml");
        }

        this.language = YamlConfiguration.loadConfiguration(languageFile);
    }

    public void send(CommandSender sender, String path) {
        sender.sendMessage(message(path));
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        sender.sendMessage(message(path, placeholders));
    }

    public void sendList(CommandSender sender, String path) {
        for (Component line : messageList(path)) {
            sender.sendMessage(line);
        }
    }

    public void sendList(CommandSender sender, String path, Map<String, String> placeholders) {
        for (Component line : messageList(path, placeholders)) {
            sender.sendMessage(line);
        }
    }

    public Component message(String path) {
        return message(path, Collections.emptyMap());
    }

    public Component message(String path, Map<String, String> placeholders) {
        String raw = language.getString(path, "<red>Missing language key: " + path + "</red>");
        return miniMessage.deserialize(applyPlaceholders(withPrefix(raw), placeholders));
    }

    public List<Component> messageList(String path) {
        return messageList(path, Collections.emptyMap());
    }

    public List<Component> messageList(String path, Map<String, String> placeholders) {
        return language.getStringList(path)
            .stream()
            .map(this::withPrefix)
            .map(line -> applyPlaceholders(line, placeholders))
            .map(miniMessage::deserialize)
            .toList();
    }

    private String withPrefix(String text) {
        String prefix = plugin.getConfig().getString("settings.prefix", "");
        return text.replace("{prefix}", prefix);
    }

    private String applyPlaceholders(String text, Map<String, String> placeholders) {
        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return result;
    }
}
