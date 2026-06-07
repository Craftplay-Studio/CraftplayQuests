package de.craftplay.quests.command;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.util.Permissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class QuestsCommand implements CommandExecutor, TabCompleter {

    private final CraftplayQuestsPlugin plugin;

    public QuestsCommand(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (args.length == 0 || is(args[0], "help", "hilfe")) {
            return sendHelp(sender);
        }

        if (is(args[0], "reload")) {
            return reload(sender);
        }

        if (is(args[0], "version", "about")) {
            plugin.language().send(sender, "commands.version", Map.of(
                "version", plugin.getPluginMeta().getVersion(),
                "server_id", plugin.serverSettings().serverId()
            ));
            return true;
        }

        plugin.language().send(sender, "commands.unknown");
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(
        @NotNull CommandSender sender,
        @NotNull Command command,
        @NotNull String label,
        @NotNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }

        List<String> suggestions = new ArrayList<>();
        suggestions.add("help");
        suggestions.add("version");

        if (sender.hasPermission(Permissions.ADMIN_RELOAD)) {
            suggestions.add("reload");
        }

        String prefix = args[0].toLowerCase(Locale.ROOT);
        return suggestions.stream()
            .filter(suggestion -> suggestion.startsWith(prefix))
            .toList();
    }

    private boolean sendHelp(CommandSender sender) {
        if (!sender.hasPermission(Permissions.USE)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }

        plugin.language().sendList(sender, "commands.help");
        return true;
    }

    private boolean reload(CommandSender sender) {
        if (!sender.hasPermission(Permissions.ADMIN_RELOAD)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }

        plugin.reloadPluginConfiguration();
        plugin.language().send(sender, "commands.reload");
        return true;
    }

    private boolean is(String input, String... options) {
        for (String option : options) {
            if (option.equalsIgnoreCase(input)) {
                return true;
            }
        }
        return false;
    }
}
