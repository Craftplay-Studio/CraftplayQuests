package de.craftplay.quests.command;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.security.ConfirmService;
import de.craftplay.quests.util.Permissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
            if (args.length >= 2) {
                return title(sender, args);
            }
            return openAdventureBook(sender);
        }

        if (is(args[0], "reload")) {
            return reload(sender);
        }

        if (is(args[0], "admin", "editor")) {
            return openAdmin(sender);
        }

        if (is(args[0], "give")) {
            return giveQuest(sender, args);
        }

        if (is(args[0], "reset")) {
            return resetQuest(sender, args);
        }

        if (is(args[0], "cache")) {
            return cache(sender, args);
        }

        if (is(args[0], "confirm")) {
            return confirm(sender, args);
        }

        if (is(args[0], "export", "backup")) {
            return export(sender, args);
        }

        if (is(args[0], "npc")) {
            return npc(sender, args);
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
            if (args.length >= 3 && sender.hasPermission(Permissions.ADMIN_QUESTS)) {
                return forceCompleteQuest(sender, args);
            }
            return playerQuestAction(sender, args, "quest.completed", Action.COMPLETE);
        }

        if (is(args[0], "decline", "ablehnen")) {
            return playerQuestAction(sender, args, "quest.canceled", Action.CANCEL);
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
            suggestions.add("admin");
            suggestions.add("book");
            suggestions.add("cancel");
            suggestions.add("cache");
            suggestions.add("complete");
            suggestions.add("completed");
            suggestions.add("confirm");
            suggestions.add("debug");
            suggestions.add("export");
            suggestions.add("give");
            suggestions.add("help");
            suggestions.add("info");
            suggestions.add("list");
            suggestions.add("npc");
            suggestions.add("progress");
            suggestions.add("reset");
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

        if (args.length == 2 && is(args[0], "cache")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return List.of("info", "clear", "delete").stream()
                .filter(suggestion -> suggestion.startsWith(prefix))
                .toList();
        }

        if (args.length == 3 && is(args[0], "cache") && is(args[1], "clear")) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
            return List.of("skins", "heads", "all").stream()
                .filter(suggestion -> suggestion.startsWith(prefix))
                .toList();
        }

        if (args.length == 2 && is(args[0], "npc")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return List.of("create", "delete", "link", "movehere", "skin", "quest", "route", "list").stream()
                .filter(suggestion -> suggestion.startsWith(prefix))
                .toList();
        }

        if (args.length == 2 && is(args[0], "give", "reset")) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix))
                .toList();
        }

        if (args.length == 3 && is(args[0], "give", "reset", "complete")) {
            String prefix = args[2].toLowerCase(Locale.ROOT);
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

    private boolean openMenu(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            return sendHelp(sender);
        }
        if (!sender.hasPermission(Permissions.MENU)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        if (plugin.services().bedrock().tryOpenFormFallback(player)) {
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
        if (plugin.services().bedrock().tryOpenFormFallback(player)) {
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
        if (plugin.services().bedrock().tryOpenFormFallback(player)) {
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
        if (plugin.services().bedrock().tryOpenFormFallback(player)) {
            return true;
        }
        plugin.services().gui().openAchievements(player);
        return true;
    }

    private boolean title(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            plugin.language().send(sender, "commands.player-only");
            return true;
        }
        if (!sender.hasPermission(Permissions.TITLE)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }

        if (is(args[1], "clear", "remove", "entfernen")) {
            plugin.services().titles().clearTitle(player.getUniqueId())
                .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                    if (throwable != null) {
                        plugin.language().send(player, "errors.generic");
                        return;
                    }
                    plugin.language().send(player, "title.cleared");
                }));
            return true;
        }

        String selectedTitle = String.join(" ", java.util.Arrays.copyOfRange(args, 1, args.length));
        plugin.services().titles().selectTitle(player.getUniqueId(), selectedTitle)
            .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                if (throwable != null) {
                    plugin.language().send(player, "title.not-unlocked");
                    return;
                }
                plugin.language().send(player, "title.selected", Map.of("title", selectedTitle));
            }));
        return true;
    }

    private boolean openAdmin(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            plugin.language().send(sender, "commands.player-only");
            return true;
        }
        if (!sender.hasPermission(Permissions.ADMIN)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        plugin.services().gui().openAdmin(player);
        plugin.language().send(sender, "admin.opened");
        plugin.services().audit().record(sender, "admin-open", "gui");
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

    private boolean giveQuest(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN_QUESTS)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        if (args.length < 3) {
            plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests give <player> <questId>"));
            return true;
        }
        UUID playerId = targetPlayerId(args[1]);
        QuestId questId = parsedQuestId(sender, args[2]);
        if (questId == null) {
            return true;
        }

        plugin.services().quests().acceptQuest(playerId, questId)
            .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                if (throwable != null) {
                    plugin.language().send(sender, "errors.generic");
                    return;
                }
                plugin.language().send(sender, "commands.admin-give", Map.of("player", args[1], "quest", questId.value()));
                plugin.services().audit().record(sender, "quest-give", args[1] + ":" + questId.value());
            }));
        return true;
    }

    private boolean resetQuest(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN_QUESTS)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        if (args.length < 3) {
            plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests reset <player> <questId>"));
            return true;
        }
        UUID playerId = targetPlayerId(args[1]);
        QuestId questId = parsedQuestId(sender, args[2]);
        if (questId == null) {
            return true;
        }

        plugin.services().quests().resetQuest(playerId, questId)
            .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                if (throwable != null) {
                    plugin.language().send(sender, "errors.generic");
                    return;
                }
                plugin.language().send(sender, "commands.admin-reset", Map.of("player", args[1], "quest", questId.value()));
                plugin.services().audit().record(sender, "quest-reset", args[1] + ":" + questId.value());
            }));
        return true;
    }

    private boolean forceCompleteQuest(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN_QUESTS)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        UUID playerId = targetPlayerId(args[1]);
        QuestId questId = parsedQuestId(sender, args[2]);
        if (questId == null) {
            return true;
        }

        plugin.services().quests().forceCompleteQuest(playerId, questId)
            .whenComplete((data, throwable) -> plugin.services().mainThread().execute(() -> {
                if (throwable != null) {
                    plugin.language().send(sender, "errors.generic");
                    return;
                }
                plugin.language().send(sender, "commands.admin-complete", Map.of("player", args[1], "quest", questId.value()));
                plugin.services().audit().record(sender, "quest-force-complete", args[1] + ":" + questId.value());
            }));
        return true;
    }

    private boolean cache(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN_CACHE)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        if (args.length < 2 || is(args[1], "info")) {
            plugin.services().cache().info()
                .whenComplete((info, throwable) -> plugin.services().mainThread().execute(() -> {
                    if (throwable != null) {
                        plugin.language().send(sender, "errors.generic");
                        return;
                    }
                    plugin.language().send(sender, "cache.info", Map.of(
                        "skins", String.valueOf(info.skins()),
                        "heads", String.valueOf(info.heads())
                    ));
                }));
            return true;
        }

        if (is(args[1], "clear")) {
            if (args.length < 3 || !is(args[2], "skins", "heads", "all")) {
                plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests cache clear <skins|heads|all>"));
                return true;
            }
            String target = args[2].toLowerCase(Locale.ROOT);
            String action = "cache-clear-" + target;
            if (plugin.services().confirmations().required("cache-clear-" + target)) {
                ConfirmService.PendingConfirmation confirmation = plugin.services().confirmations().request(sender, action, target);
                plugin.language().send(sender, "confirm.generated", Map.of(
                    "code", confirmation.code(),
                    "seconds", String.valueOf(Math.max(5, plugin.getConfig().getInt("confirm.expire-seconds", 60)))
                ));
                return true;
            }
            executeConfirmedAction(sender, action, target);
            return true;
        }

        plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests cache <info|clear>"));
        return true;
    }

    private boolean confirm(CommandSender sender, String[] args) {
        if (args.length < 2) {
            plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests confirm <code>"));
            return true;
        }
        plugin.services().confirmations().confirm(sender, args[1]).ifPresentOrElse(
            confirmation -> executeConfirmedAction(sender, confirmation.action(), confirmation.detail()),
            () -> plugin.language().send(sender, "confirm.invalid")
        );
        return true;
    }

    private boolean export(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        boolean backup = args.length > 0 && is(args[0], "backup");
        var future = backup ? plugin.services().exports().backupSaveData() : plugin.services().exports().exportSaveData();
        future.whenComplete((file, throwable) -> plugin.services().mainThread().execute(() -> {
            if (throwable != null) {
                plugin.language().send(sender, "errors.generic");
                return;
            }
            plugin.language().send(sender, "commands.export-created", Map.of("file", file.toString()));
            plugin.services().audit().record(sender, backup ? "backup" : "export", file.toString());
        }));
        return true;
    }

    private boolean npc(CommandSender sender, String[] args) {
        if (!sender.hasPermission(Permissions.ADMIN_NPCS)) {
            plugin.language().send(sender, "commands.no-permission");
            return true;
        }
        if (args.length < 2) {
            plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests npc <create|delete|link|movehere|skin|quest|route|list>"));
            return true;
        }

        if (is(args[1], "list")) {
            for (var npc : plugin.services().npcs().all()) {
                plugin.language().send(sender, "commands.npc-list-entry", Map.of(
                    "npc", npc.id(),
                    "name", npc.displayName(),
                    "quests", String.valueOf(npc.quests().size())
                ));
            }
            return true;
        }

        if (is(args[1], "create")) {
            if (args.length < 4) {
                plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests npc create <id> <name>"));
                return true;
            }
            String name = String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length));
            plugin.services().npcs().create(args[2], name).whenComplete((npc, throwable) -> npcDone(sender, throwable, "npc.created", Map.of("npc", args[2]), "npc-create", args[2]));
            return true;
        }

        if (is(args[1], "delete")) {
            if (args.length < 3) {
                plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests npc delete <id>"));
                return true;
            }
            plugin.services().npcs().delete(args[2]).whenComplete((deleted, throwable) -> npcDone(sender, throwable, "npc.deleted", Map.of("npc", args[2]), "npc-delete", args[2]));
            return true;
        }

        if (is(args[1], "link")) {
            if (args.length < 4) {
                plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests npc link <id> <citizensId>"));
                return true;
            }
            int citizensId = Integer.parseInt(args[3]);
            plugin.services().npcs().linkCitizens(args[2], citizensId).whenComplete((npc, throwable) -> npcDone(sender, throwable, "npc.quest-added", Map.of("npc", args[2], "quest", "Citizens-" + citizensId), "npc-link", args[2]));
            return true;
        }

        if (is(args[1], "movehere")) {
            if (!(sender instanceof Player player)) {
                plugin.language().send(sender, "commands.player-only");
                return true;
            }
            if (args.length < 3) {
                plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests npc movehere <id>"));
                return true;
            }
            plugin.services().npcs().setLocation(args[2], player.getLocation()).whenComplete((npc, throwable) -> npcDone(sender, throwable, "npc.moved-here", Map.of("npc", args[2]), "npc-movehere", args[2]));
            return true;
        }

        if (is(args[1], "skin")) {
            if (args.length < 4) {
                plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests npc skin <id> <minecraftName>"));
                return true;
            }
            plugin.services().npcs().setSkin(args[2], args[3]).whenComplete((npc, throwable) -> npcDone(sender, throwable, "npc.skin-updated", Map.of("npc", args[2]), "npc-skin", args[2] + ":" + args[3]));
            return true;
        }

        if (is(args[1], "quest")) {
            return npcQuest(sender, args);
        }

        if (is(args[1], "route")) {
            if (!(sender instanceof Player player)) {
                plugin.language().send(sender, "commands.player-only");
                return true;
            }
            if (args.length < 4 || !is(args[2], "addpoint")) {
                plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests npc route addpoint <id>"));
                return true;
            }
            plugin.services().npcs().addRoutePoint(args[3], player.getLocation()).whenComplete((npc, throwable) -> npcDone(sender, throwable, "npc.route-point-added", Map.of(), "npc-route-addpoint", args[3]));
            return true;
        }

        plugin.language().send(sender, "commands.unknown");
        return true;
    }

    private boolean npcQuest(CommandSender sender, String[] args) {
        if (args.length < 5 || !is(args[2], "add", "remove")) {
            plugin.language().send(sender, "commands.usage", Map.of("usage", "/quests npc quest <add|remove> <npcId> <questId>"));
            return true;
        }

        QuestId questId = parsedQuestId(sender, args[4]);
        if (questId == null) {
            return true;
        }

        if (is(args[2], "add")) {
            plugin.services().npcs().addQuest(args[3], questId)
                .whenComplete((npc, throwable) -> npcDone(sender, throwable, "npc.quest-added", Map.of("npc", args[3], "quest", questId.value()), "npc-quest-add", args[3] + ":" + questId.value()));
            return true;
        }

        plugin.services().npcs().removeQuest(args[3], questId)
            .whenComplete((npc, throwable) -> npcDone(sender, throwable, "npc.quest-removed", Map.of("npc", args[3], "quest", questId.value()), "npc-quest-remove", args[3] + ":" + questId.value()));
        return true;
    }

    private void npcDone(CommandSender sender, Throwable throwable, String message, Map<String, String> placeholders, String auditAction, String detail) {
        plugin.services().mainThread().execute(() -> {
            if (throwable != null) {
                plugin.language().send(sender, "errors.generic");
                return;
            }
            plugin.language().send(sender, message, placeholders);
            plugin.services().audit().record(sender, auditAction, detail);
        });
    }

    private void executeConfirmedAction(CommandSender sender, String action, String detail) {
        CompletableFutureAction futureAction = switch (action) {
            case "cache-clear-skins" -> new CompletableFutureAction(plugin.services().cache().clearSkins(), "skins");
            case "cache-clear-heads" -> new CompletableFutureAction(plugin.services().cache().clearHeads(), "heads");
            case "cache-clear-all" -> new CompletableFutureAction(plugin.services().cache().clearAll(), "all");
            default -> null;
        };

        if (futureAction == null) {
            plugin.language().send(sender, "confirm.invalid");
            return;
        }

        futureAction.future().whenComplete((ignored, throwable) -> plugin.services().mainThread().execute(() -> {
            if (throwable != null) {
                plugin.language().send(sender, "errors.generic");
                return;
            }
            plugin.language().send(sender, "confirm.success");
            plugin.language().send(sender, "cache.cleared", Map.of("cache", futureAction.cacheName()));
            plugin.services().audit().record(sender, action, detail);
        }));
    }

    private UUID targetPlayerId(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
            Player online = Bukkit.getPlayerExact(input);
            if (online != null) {
                return online.getUniqueId();
            }
            OfflinePlayer offline = Bukkit.getOfflinePlayer(input);
            return offline.getUniqueId();
        }
    }

    private QuestId parsedQuestId(CommandSender sender, String value) {
        try {
            QuestId questId = QuestId.of(value);
            if (plugin.services().quests().findQuest(questId).isEmpty()) {
                plugin.language().send(sender, "quest.not-found", Map.of("quest", value));
                return null;
            }
            return questId;
        } catch (IllegalArgumentException exception) {
            plugin.language().send(sender, "quest.not-found", Map.of("quest", value));
            return null;
        }
    }

    private record CompletableFutureAction(java.util.concurrent.CompletableFuture<Void> future, String cacheName) {
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
