package de.craftplay.quests.command;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.util.Permissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
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

        if (is(args[0], "accept", "annehmen")) {
            return playerQuestAction(sender, args, "quest.accepted", Action.ACCEPT);
        }

        if (is(args[0], "complete", "abschliessen", "abschließen")) {
            return playerQuestAction(sender, args, "quest.completed", Action.COMPLETE);
        }

        if (is(args[0], "cancel", "abbrechen")) {
            return playerQuestAction(sender, args, "quest.canceled", Action.CANCEL);
        }

        if (is(args[0], "track", "verfolgen")) {
            return playerQuestAction(sender, args, "quest.tracked", Action.TRACK);
        }

        if (is(args[0], "untrack")) {
            return untrack(sender);
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
        if (args.length == 1) {
            List<String> suggestions = new ArrayList<>();
            suggestions.add("accept");
            suggestions.add("cancel");
            suggestions.add("complete");
            suggestions.add("help");
            suggestions.add("track");
            suggestions.add("untrack");
            suggestions.add("version");

            if (sender.hasPermission(Permissions.ADMIN_RELOAD)) {
                suggestions.add("reload");
            }

            String prefix = args[0].toLowerCase(Locale.ROOT);
            return suggestions.stream()
                .filter(suggestion -> suggestion.startsWith(prefix))
                .toList();
        }

        if (args.length == 2 && is(args[0], "accept", "annehmen", "complete", "abschliessen", "abschließen", "cancel", "abbrechen", "track", "verfolgen")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.services().quests().registry().sortedById().stream()
                .map(Quest::id)
                .map(QuestId::value)
                .filter(questId -> questId.startsWith(prefix))
                .toList();
        }

        return List.of();
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

    private boolean playerQuestAction(CommandSender sender, String[] args, String successPath, Action action) {
        if (!(sender instanceof Player player)) {
            plugin.language().send(sender, "commands.player-only");
            return true;
        }
        if (!sender.hasPermission(Permissions.USE)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests " + args[0] + " <questId>"));
            return true;
        }

        QuestId questId;
        try {
            questId = QuestId.of(args[1]);
        } catch (IllegalArgumentException exception) {
            plugin.language().send(sender, "quest.not-found", Map.of("quest", args[1]));
            return true;
        }

        var future = switch (action) {
            case ACCEPT -> plugin.services().quests().acceptQuest(player.getUniqueId(), questId);
            case COMPLETE -> plugin.services().quests().completeQuest(player.getUniqueId(), questId);
            case CANCEL -> plugin.services().quests().cancelQuest(player.getUniqueId(), questId);
            case TRACK -> plugin.services().quests().trackQuest(player.getUniqueId(), questId);
        };

        future.whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
            if (throwable != null) {
                plugin.language().send(player, "errors.generic");
                return;
            }
            plugin.language().send(player, successPath, Map.of("quest", questId.value()));
        }));
        return true;
    }

    private boolean untrack(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.language().send(sender, "commands.player-only");
            return true;
        }
        if (!sender.hasPermission(Permissions.USE)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }

        plugin.services().quests().untrackQuest(player.getUniqueId())
            .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                if (throwable != null) {
                    plugin.language().send(player, "errors.generic");
                    return;
                }
                plugin.language().send(player, "quest.untracked");
            }));
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

    private enum Action {
        ACCEPT,
        COMPLETE,
        CANCEL,
        TRACK
    }
}
