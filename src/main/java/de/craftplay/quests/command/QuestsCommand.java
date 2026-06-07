package de.craftplay.quests.command;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.util.Permissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
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
        if (args.length == 0) {
            return openMenu(sender);
        }

        if (is(args[0], "help", "hilfe")) {
            return sendHelp(sender);
        }

        if (is(args[0], "gui", "menu", "menü")) {
            return openMenu(sender);
        }

        if (is(args[0], "active", "completed")) {
            return openPlayerQuests(sender);
        }

        if (is(args[0], "book", "buch")) {
            return openAdventureBook(sender);
        }

        if (is(args[0], "achievements", "erfolge")) {
            return openAchievements(sender);
        }

        if (is(args[0], "title", "titel")) {
            return openAdventureBook(sender);
        }

        if (is(args[0], "reload")) {
            return reload(sender);
        }

        if (is(args[0], "list", "liste")) {
            return listQuests(sender);
        }

        if (is(args[0], "info")) {
            return questInfo(sender, args);
        }

        if (is(args[0], "progress", "fortschritt")) {
            return progress(sender, args);
        }

        if (is(args[0], "debug")) {
            return debug(sender, args);
        }

        if (is(args[0], "import")) {
            return importData(sender, args);
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
            suggestions.add("achievements");
            suggestions.add("active");
            suggestions.add("book");
            suggestions.add("cancel");
            suggestions.add("complete");
            suggestions.add("completed");
            suggestions.add("debug");
            suggestions.add("help");
            suggestions.add("info");
            suggestions.add("list");
            suggestions.add("progress");
            suggestions.add("title");
            suggestions.add("track");
            suggestions.add("untrack");
            suggestions.add("version");

            if (sender.hasPermission(Permissions.ADMIN_RELOAD)) {
                suggestions.add("reload");
            }
            if (sender.hasPermission(Permissions.ADMIN_IMPORT)) {
                suggestions.add("import");
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

        if (args.length == 2 && is(args[0], "info")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.services().quests().registry().sortedById().stream()
                .map(Quest::id)
                .map(QuestId::value)
                .filter(questId -> questId.startsWith(prefix))
                .toList();
        }

        if (args.length == 2 && is(args[0], "debug")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return List.of("hooks", "services", "storage", "version").stream()
                .filter(suggestion -> suggestion.startsWith(prefix))
                .toList();
        }

        if (args.length == 2 && is(args[0], "import")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return List.of("questsplugin").stream()
                .filter(suggestion -> suggestion.startsWith(prefix))
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

    private boolean openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return sendHelp(sender);
        }
        if (!sender.hasPermission(Permissions.MENU)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        plugin.services().gui().openMain(player);
        return true;
    }

    private boolean openPlayerQuests(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.language().send(sender, "commands.player-only");
            return true;
        }
        if (!sender.hasPermission(Permissions.MENU)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        plugin.services().gui().openPlayerQuests(player);
        return true;
    }

    private boolean openAdventureBook(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.language().send(sender, "commands.player-only");
            return true;
        }
        if (!sender.hasPermission(Permissions.BOOK)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        plugin.services().gui().openAdventureBook(player);
        return true;
    }

    private boolean openAchievements(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.language().send(sender, "commands.player-only");
            return true;
        }
        if (!sender.hasPermission(Permissions.ACHIEVEMENTS)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        plugin.services().gui().openAchievements(player);
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

    private boolean listQuests(CommandSender sender) {
        if (!sender.hasPermission(Permissions.USE)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }

        List<Quest> quests = plugin.services().quests().registry().sortedById();
        plugin.language().send(sender, "commands.quest-list-header", Map.of("count", String.valueOf(quests.size())));
        for (Quest quest : quests) {
            plugin.language().send(sender, "commands.quest-list-entry", Map.of(
                "quest", quest.id().value(),
                "name", quest.name(),
                "type", quest.metadata().type().name(),
                "enabled", String.valueOf(quest.enabled())
            ));
        }
        return true;
    }

    private boolean questInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.USE)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests info <questId>"));
            return true;
        }

        QuestId questId;
        try {
            questId = QuestId.of(args[1]);
        } catch (IllegalArgumentException exception) {
            plugin.language().send(sender, "quest.not-found", Map.of("quest", args[1]));
            return true;
        }

        Quest quest = plugin.services().quests().findQuest(questId).orElse(null);
        if (quest == null) {
            plugin.language().send(sender, "quest.not-found", Map.of("quest", questId.value()));
            return true;
        }

        plugin.language().send(sender, "commands.quest-info", Map.of(
            "quest", quest.id().value(),
            "name", quest.name(),
            "category", quest.metadata().category(),
            "type", quest.metadata().type().name(),
            "difficulty", quest.metadata().difficulty().name(),
            "objectives", String.valueOf(quest.objectives().size()),
            "requirements", String.valueOf(quest.requirements().size()),
            "rewards", String.valueOf(quest.rewards().size()),
            "enabled", String.valueOf(quest.enabled())
        ));
        return true;
    }

    private boolean progress(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.USE)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }

        Player target;
        if (args.length >= 2) {
            if (!sender.hasPermission(Permissions.ADMIN_QUESTS)) {
                plugin.language().send(sender, "commands.no-permission");
                return true;
            }
            target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                plugin.language().send(sender, "errors.invalid-player", Map.of("player", args[1]));
                return true;
            }
        } else if (sender instanceof Player player) {
            target = player;
        } else {
            plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests progress <player>"));
            return true;
        }

        UUID targetId = target.getUniqueId();
        String targetName = target.getName();
        plugin.services().quests().playerData(targetId)
            .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                if (throwable != null) {
                    plugin.language().send(sender, "errors.generic");
                    return;
                }
                plugin.language().send(sender, "commands.player-progress", Map.of(
                    "player", targetName,
                    "active", String.valueOf(data.activeQuests().size()),
                    "completed", String.valueOf(data.completedQuests().size()),
                    "points", String.valueOf(data.questPoints()),
                    "reputation", String.valueOf(data.reputation()),
                    "achievements", String.valueOf(data.achievements().size()),
                    "title", data.unlockedTitles().stream().findFirst().orElse("-")
                ));
            }));
        return true;
    }

    private boolean debug(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }

        String section = args.length >= 2 ? args[1].toLowerCase(Locale.ROOT) : "services";
        if ("hooks".equals(section)) {
            for (Map.Entry<String, Boolean> hook : plugin.services().hooks().snapshot().entrySet()) {
                plugin.language().send(sender, "commands.debug-hook", Map.of(
                    "hook", hook.getKey(),
                    "enabled", String.valueOf(hook.getValue())
                ));
            }
            return true;
        }

        if ("storage".equals(section) || "services".equals(section)) {
            plugin.language().send(sender, "commands.debug-storage", Map.of(
                "storage", plugin.services().storage().activeProviderId(),
                "quests", String.valueOf(plugin.services().quests().registry().size()),
                "api", String.valueOf(plugin.services().webApi().running())
            ));
        }

        if ("version".equals(section) || "services".equals(section)) {
            plugin.language().send(sender, "commands.debug-version", Map.of(
                "adapter", plugin.services().version().family().name(),
                "version", plugin.services().version().serverVersion(),
                "text_display", String.valueOf(plugin.services().version().supportsTextDisplay())
            ));
        }

        return true;
    }

    private boolean importData(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN_IMPORT)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        if (args.length < 2 || !is(args[1], "questsplugin")) {
            plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests import questsplugin"));
            return true;
        }

        plugin.language().send(sender, "import.started", Map.of("importer", "QuestsPlugin"));
        plugin.services().imports().importQuestsPlugin()
            .whenComplete((report, throwable) -> plugin.services().mainThread().execute(() -> {
                if (throwable != null) {
                    plugin.language().send(sender, "import.failed", Map.of("error", throwable.getMessage()));
                    return;
                }
                plugin.language().send(sender, "import.report-created", Map.of("file", report));
            }));
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
