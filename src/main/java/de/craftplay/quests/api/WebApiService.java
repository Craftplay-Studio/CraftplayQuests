package de.craftplay.quests.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.npc.NpcData;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.player.PlayerQuestData;
import de.craftplay.quests.quest.storage.PlayerQuestDataSerializer;
import de.craftplay.quests.quest.storage.QuestSerializer;
import de.craftplay.quests.stats.PlayerRankEntry;
import de.craftplay.quests.stats.StatsSnapshot;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class WebApiService {

    private final CraftplayQuestsPlugin plugin;
    private final QuestSerializer questSerializer = new QuestSerializer();
    private final PlayerQuestDataSerializer playerDataSerializer = new PlayerQuestDataSerializer();
    private final ConcurrentHashMap<String, RateWindow> rateWindows = new ConcurrentHashMap<>();
    private HttpServer server;
    private ExecutorService executor;

    public WebApiService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public synchronized void start() {
        if (!plugin.getConfig().getBoolean("api.enabled", false) || server != null) {
            return;
        }

        String host = plugin.getConfig().getString("api.host", "127.0.0.1");
        int port = plugin.getConfig().getInt("api.port", 8721);
        int workers = Math.max(1, plugin.getConfig().getInt("api.worker-threads", 2));

        try {
            HttpServer createdServer = HttpServer.create(new InetSocketAddress(host, port), 25);
            AtomicInteger threadId = new AtomicInteger();
            executor = Executors.newFixedThreadPool(workers, runnable -> {
                Thread thread = new Thread(runnable, "craftplayquests-api-" + threadId.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });
            createdServer.setExecutor(executor);
            createdServer.createContext("/api/health", this::handleHealth);
            createdServer.createContext("/api/stats/overview", this::handleOverview);
            createdServer.createContext("/api/stats/top-players", this::handleTopPlayers);
            createdServer.createContext("/api/stats/player", this::handlePlayerStats);
            createdServer.createContext("/api/admin/quests", this::handleQuests);
            createdServer.createContext("/api/admin/npcs", this::handleNpcs);
            createdServer.createContext("/api/admin/playerdata", this::handlePlayerData);
            createdServer.createContext("/api/admin/titles", this::handleTitles);
            createdServer.createContext("/api/admin/achievements", this::handleAchievements);
            createdServer.createContext("/api/admin/advancements", this::handleAdvancements);
            createdServer.createContext("/api/admin/hooks", this::handleHooks);
            createdServer.createContext("/api/admin/import", this::handleImport);
            createdServer.createContext("/api/admin/cache", this::handleCache);
            createdServer.createContext("/api/admin/export", this::handleExport);
            createdServer.createContext("/api/admin/storage/flush", this::handleStorageFlush);
            createdServer.createContext("/api/admin/reload", this::handleReload);
            createdServer.start();
            server = createdServer;
            plugin.getLogger().info("CraftplayQuests API started on " + host + ":" + port);
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not start CraftplayQuests API: " + exception.getMessage());
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
        }
    }

    public synchronized void stop() {
        HttpServer currentServer = server;
        server = null;
        if (currentServer != null) {
            currentServer.stop(2);
            plugin.getLogger().info("CraftplayQuests API stopped.");
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public boolean running() {
        return server != null;
    }

    private void handleHealth(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!plugin.getConfig().getBoolean("api.allow-health-without-token", true) && !authorized(exchange, "panel", "homepage")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }
        sendJson(exchange, 200, "{\"status\":\"ok\",\"plugin\":\"CraftplayQuests\"}");
    }

    private void handleOverview(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel", "homepage")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }

        StatsSnapshot stats = plugin.services().stats().snapshot().join();
        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"serverId\":\"").append(escape(plugin.serverSettings().serverId())).append("\",");
        json.append("\"displayName\":\"").append(escape(plugin.serverSettings().displayName())).append("\",");
        json.append("\"version\":\"").append(escape(plugin.getPluginMeta().getVersion())).append("\",");
        json.append("\"storage\":\"").append(escape(plugin.services().storage().activeProviderId())).append("\",");
        json.append("\"minecraftVersion\":\"").append(escape(plugin.services().version().serverVersion())).append("\",");
        json.append("\"versionFamily\":\"").append(escape(plugin.services().version().family().name())).append("\",");
        json.append("\"quests\":").append(plugin.services().quests().registry().size()).append(',');
        json.append("\"knownPlayers\":").append(stats.knownPlayers()).append(',');
        json.append("\"activeQuestEntries\":").append(stats.activeQuestEntries()).append(',');
        json.append("\"completedQuestEntries\":").append(stats.completedQuestEntries()).append(',');
        json.append("\"questPoints\":").append(stats.questPoints()).append(',');
        json.append("\"reputation\":").append(stats.reputation()).append(',');
        json.append("\"achievements\":").append(stats.achievements()).append(',');
        json.append("\"queuedWrites\":").append(plugin.services().storage().queuedWrites()).append(',');
        json.append("\"redis\":").append(plugin.services().redis().available()).append(',');
        json.append("\"apiRunning\":").append(running()).append(',');
        json.append("\"hooks\":").append(hooksJson());
        json.append('}');
        sendJson(exchange, 200, json.toString());
    }

    private void handleQuests(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String questId = suffix(path, "/api/admin/quests");
        String method = exchange.getRequestMethod().toUpperCase(java.util.Locale.ROOT);
        if ("GET".equals(method) && !questId.isBlank()) {
            Quest quest = plugin.services().quests().findQuest(QuestId.of(questId)).orElse(null);
            if (quest == null) {
                sendJson(exchange, 404, "{\"error\":\"quest-not-found\"}");
                return;
            }
            sendJson(exchange, 200, questJson(quest));
            return;
        }
        if ("POST".equals(method) || "PUT".equals(method)) {
            String body = readBody(exchange);
            Quest quest = questSerializer.deserialize(body);
            if (!questId.isBlank() && !quest.id().value().equalsIgnoreCase(questId)) {
                sendJson(exchange, 400, "{\"error\":\"quest-id-mismatch\"}");
                return;
            }
            plugin.services().quests().saveQuest(quest).join();
            plugin.services().audit().record("api", "quest-save", quest.id().value());
            sendJson(exchange, 200, questJson(quest));
            return;
        }
        if ("DELETE".equals(method) && !questId.isBlank()) {
            boolean deleted = plugin.services().quests().deleteQuest(QuestId.of(questId)).join();
            plugin.services().audit().record("api", "quest-delete", questId);
            sendJson(exchange, deleted ? 200 : 404, "{\"deleted\":" + deleted + "}");
            return;
        }
        if (!"GET".equals(method)) {
            sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append('{').append("\"quests\":[");
        boolean first = true;
        for (Quest quest : plugin.services().quests().registry().sortedById()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(questJson(quest));
        }
        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    private void handleTopPlayers(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel", "homepage")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }
        StatsSnapshot snapshot = plugin.services().stats().snapshot().join();
        StringBuilder json = new StringBuilder("{\"players\":[");
        boolean first = true;
        for (PlayerRankEntry entry : snapshot.topPlayers()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(playerRankJson(entry));
        }
        json.append("]}");
        sendJson(exchange, 200, json.toString());
    }

    private void handlePlayerStats(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel", "homepage")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }
        String playerId = suffix(exchange.getRequestURI().getPath(), "/api/stats/player");
        if (playerId.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"player-required\"}");
            return;
        }
        Optional<UUID> parsedPlayerId = uuid(playerId);
        if (parsedPlayerId.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"valid-player-uuid-required\"}");
            return;
        }
        PlayerQuestData data = plugin.services().stats().player(parsedPlayerId.get()).join();
        sendJson(exchange, 200, playerDataJson(data));
    }

    private void handleNpcs(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }

        String path = exchange.getRequestURI().getPath();
        String npcId = suffix(path, "/api/admin/npcs");
        String method = exchange.getRequestMethod().toUpperCase(java.util.Locale.ROOT);
        if ("GET".equals(method)) {
            StringBuilder json = new StringBuilder("{\"npcs\":[");
            boolean first = true;
            for (NpcData npc : plugin.services().npcs().all()) {
                if (!first) {
                    json.append(',');
                }
                first = false;
                json.append(npcJson(npc));
            }
            json.append("]}");
            sendJson(exchange, 200, json.toString());
            return;
        }
        if ("POST".equals(method)) {
            Map<String, String> query = query(exchange);
            String id = query.getOrDefault("id", "");
            String name = query.getOrDefault("name", id);
            if (id.isBlank()) {
                sendJson(exchange, 400, "{\"error\":\"npc-id-required\"}");
                return;
            }
            NpcData npc = plugin.services().npcs().create(id, name).join();
            plugin.services().audit().record("api", "npc-create", id);
            sendJson(exchange, 200, npcJson(npc));
            return;
        }
        if ("DELETE".equals(method) && !npcId.isBlank()) {
            boolean deleted = plugin.services().npcs().delete(npcId).join();
            plugin.services().audit().record("api", "npc-delete", npcId);
            sendJson(exchange, deleted ? 200 : 404, "{\"deleted\":" + deleted + "}");
            return;
        }
        if ("PUT".equals(method) && !npcId.isBlank()) {
            Map<String, String> query = query(exchange);
            String action = query.getOrDefault("action", "").toLowerCase(java.util.Locale.ROOT);
            try {
                NpcData npc = switch (action) {
                    case "link" -> plugin.services().npcs().linkCitizens(npcId, Integer.parseInt(query.getOrDefault("citizensId", "-1"))).join();
                    case "skin" -> plugin.services().npcs().setSkin(npcId, query.getOrDefault("skin", "")).join();
                    case "quest-add" -> plugin.services().npcs().addQuest(npcId, QuestId.of(query.getOrDefault("quest", ""))).join();
                    case "quest-remove" -> plugin.services().npcs().removeQuest(npcId, QuestId.of(query.getOrDefault("quest", ""))).join();
                    default -> null;
                };
                if (npc == null) {
                    sendJson(exchange, 400, "{\"error\":\"unsupported-npc-action\"}");
                    return;
                }
                plugin.services().audit().record("api", "npc-" + action, npcId);
                sendJson(exchange, 200, npcJson(npc));
            } catch (RuntimeException exception) {
                sendJson(exchange, 400, "{\"error\":\"invalid-npc-update\"}");
            }
            return;
        }
        sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
    }

    private void handlePlayerData(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }

        String playerIdValue = suffix(exchange.getRequestURI().getPath(), "/api/admin/playerdata");
        String method = exchange.getRequestMethod().toUpperCase(java.util.Locale.ROOT);
        if ("GET".equals(method) && playerIdValue.isBlank()) {
            List<PlayerQuestData> players = plugin.services().quests().allPlayerData().join();
            sendJson(exchange, 200, "{\"players\":" + playerDataArrayJson(players) + "}");
            return;
        }

        Optional<UUID> playerId = uuid(playerIdValue);
        if (playerId.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"valid-player-uuid-required\"}");
            return;
        }

        if ("GET".equals(method)) {
            PlayerQuestData data = plugin.services().quests().playerData(playerId.get()).join();
            sendJson(exchange, 200, playerDataDetailedJson(data));
            return;
        }
        if ("POST".equals(method) || "PUT".equals(method)) {
            String body = readBody(exchange);
            PlayerQuestData data = body.isBlank()
                ? PlayerQuestData.empty(playerId.get())
                : playerDataSerializer.deserialize(playerId.get(), body);
            PlayerQuestData saved = plugin.services().quests().savePlayerData(data).join();
            plugin.services().audit().record("api", "playerdata-save", playerId.get().toString());
            sendJson(exchange, 200, playerDataDetailedJson(saved));
            return;
        }
        if ("DELETE".equals(method)) {
            PlayerQuestData saved = plugin.services().quests().savePlayerData(PlayerQuestData.empty(playerId.get())).join();
            plugin.services().audit().record("api", "playerdata-reset", playerId.get().toString());
            sendJson(exchange, 200, playerDataDetailedJson(saved));
            return;
        }
        sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
    }

    private void handleTitles(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }

        Map<String, String> query = query(exchange);
        String playerValue = query.getOrDefault("player", suffix(exchange.getRequestURI().getPath(), "/api/admin/titles"));
        String method = exchange.getRequestMethod().toUpperCase(java.util.Locale.ROOT);
        if ("GET".equals(method) && playerValue.isBlank()) {
            sendJson(exchange, 200, titleStatsJson(plugin.services().quests().allPlayerData().join()));
            return;
        }

        Optional<UUID> playerId = uuid(playerValue);
        if (playerId.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"valid-player-uuid-required\"}");
            return;
        }

        if ("GET".equals(method)) {
            sendJson(exchange, 200, playerDataDetailedJson(plugin.services().quests().playerData(playerId.get()).join()));
            return;
        }
        if (!"POST".equals(method) && !"PUT".equals(method) && !"DELETE".equals(method)) {
            sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
            return;
        }

        String title = query.getOrDefault("title", "");
        String action = "DELETE".equals(method) ? "clear" : query.getOrDefault("action", "grant").toLowerCase(java.util.Locale.ROOT);
        if (("grant".equals(action) || "select".equals(action)) && title.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"title-required\"}");
            return;
        }
        PlayerQuestData updated;
        try {
            updated = switch (action) {
                case "select" -> plugin.services().titles().selectTitle(playerId.get(), title).join();
                case "clear", "delete" -> plugin.services().titles().clearTitle(playerId.get()).join();
                case "grant" -> plugin.services().titles().grantTitle(playerId.get(), title).join();
                default -> null;
            };
        } catch (RuntimeException exception) {
            sendJson(exchange, 400, "{\"error\":\"invalid-title-action\"}");
            return;
        }
        if (updated == null) {
            sendJson(exchange, 400, "{\"error\":\"unsupported-title-action\"}");
            return;
        }
        plugin.services().audit().record("api", "title-" + action, playerId.get() + ":" + title);
        sendJson(exchange, 200, playerDataDetailedJson(updated));
    }

    private void handleAchievements(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }

        Map<String, String> query = query(exchange);
        String playerValue = query.getOrDefault("player", suffix(exchange.getRequestURI().getPath(), "/api/admin/achievements"));
        String method = exchange.getRequestMethod().toUpperCase(java.util.Locale.ROOT);
        if ("GET".equals(method) && playerValue.isBlank()) {
            sendJson(exchange, 200, achievementStatsJson(plugin.services().quests().allPlayerData().join()));
            return;
        }

        Optional<UUID> playerId = uuid(playerValue);
        if (playerId.isEmpty()) {
            sendJson(exchange, 400, "{\"error\":\"valid-player-uuid-required\"}");
            return;
        }

        if ("GET".equals(method)) {
            sendJson(exchange, 200, playerDataDetailedJson(plugin.services().quests().playerData(playerId.get()).join()));
            return;
        }
        if (!"POST".equals(method) && !"PUT".equals(method)) {
            sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
            return;
        }

        String achievement = query.getOrDefault("achievement", "");
        if (achievement.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"achievement-required\"}");
            return;
        }
        PlayerQuestData updated = plugin.services().achievements().unlock(playerId.get(), achievement).join();
        plugin.services().audit().record("api", "achievement-unlock", playerId.get() + ":" + achievement);
        sendJson(exchange, 200, playerDataDetailedJson(updated));
    }

    private void handleAdvancements(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }

        String method = exchange.getRequestMethod().toUpperCase(java.util.Locale.ROOT);
        if ("GET".equals(method)) {
            sendJson(exchange, 200, advancementSnapshotJson());
            return;
        }
        if (!"POST".equals(method) && !"PUT".equals(method)) {
            sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
            return;
        }

        Map<String, String> query = query(exchange);
        String achievement = query.getOrDefault("achievement", query.getOrDefault("id", ""));
        if (achievement.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"achievement-required\"}");
            return;
        }
        String title = query.getOrDefault("title", achievement);
        String description = query.getOrDefault("description", achievement);
        String icon = query.getOrDefault("icon", "minecraft:book");
        plugin.services().advancements().registerAchievement(achievement, title, description, icon).join();
        plugin.services().audit().record("api", "advancement-register", achievement);
        sendJson(exchange, 200, advancementSnapshotJson());
    }

    private void handleHooks(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
            return;
        }
        sendJson(exchange, 200, "{\"hooks\":" + hooksJson() + "}");
    }

    private void handleStorageFlush(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
            return;
        }
        plugin.services().storage().flushDirtyQueue().join();
        plugin.services().audit().record("api", "storage-flush", "dirty-queue");
        sendJson(exchange, 200, "{\"flushed\":true,\"queuedWrites\":" + plugin.services().storage().queuedWrites() + "}");
    }

    private void handleReload(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
            return;
        }
        plugin.services().mainThread().supply(() -> {
            plugin.reloadPluginConfiguration();
            return true;
        }).join();
        plugin.services().audit().record("api", "reload", "configuration");
        sendJson(exchange, 200, "{\"reloaded\":true}");
    }

    private void handleImport(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
            return;
        }
        String report = plugin.services().imports().importQuestsPlugin().join();
        plugin.services().audit().record("api", "import-questsplugin", report);
        sendJson(exchange, 200, "{\"report\":\"" + escape(report) + "\"}");
    }

    private void handleCache(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
            return;
        }
        var info = plugin.services().cache().info().join();
        sendJson(exchange, 200, "{\"skins\":" + info.skins() + ",\"heads\":" + info.heads() + "}");
    }

    private void handleExport(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange, "panel")) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
            return;
        }
        String file = plugin.services().exports().exportSaveData().join().toString();
        plugin.services().audit().record("api", "export", file);
        sendJson(exchange, 200, "{\"file\":\"" + escape(file) + "\"}");
    }

    private boolean authorized(HttpExchange exchange, String... allowedTokenNames) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return plugin.serverSettings().hasApiToken(authorization.substring(7).trim(), allowedTokenNames);
        }

        Map<String, String> query = query(exchange);
        return plugin.serverSettings().hasApiToken(query.get("token"), allowedTokenNames);
    }

    private boolean rateLimited(HttpExchange exchange) {
        int limit = plugin.getConfig().getInt("api.rate-limit-per-minute", 120);
        if (limit < 1) {
            return false;
        }
        String address = exchange.getRemoteAddress() == null ? "unknown" : exchange.getRemoteAddress().getAddress().getHostAddress();
        long minute = System.currentTimeMillis() / 60000L;
        RateWindow window = rateWindows.compute(address, (key, current) -> {
            if (current == null || current.minute() != minute) {
                return new RateWindow(minute, 1);
            }
            return new RateWindow(minute, current.count() + 1);
        });
        return window.count() > limit;
    }

    private Map<String, String> query(HttpExchange exchange) {
        String rawQuery = exchange.getRequestURI().getRawQuery();
        if (rawQuery == null || rawQuery.isBlank()) {
            return Map.of();
        }

        Map<String, String> result = new LinkedHashMap<>();
        for (String pair : rawQuery.split("&")) {
            int separator = pair.indexOf('=');
            String key = separator < 0 ? pair : pair.substring(0, separator);
            String value = separator < 0 ? "" : pair.substring(separator + 1);
            result.put(urlDecode(key), urlDecode(value));
        }
        return result;
    }

    private String hooksJson() {
        StringBuilder json = new StringBuilder();
        json.append('{');
        boolean first = true;
        for (Map.Entry<String, Boolean> entry : plugin.services().hooks().snapshot().entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(escape(entry.getKey())).append("\":").append(entry.getValue());
        }
        json.append('}');
        return json.toString();
    }

    private String questJson(Quest quest) {
        return new StringBuilder()
            .append('{')
            .append("\"id\":\"").append(escape(quest.id().value())).append("\",")
            .append("\"name\":\"").append(escape(quest.name())).append("\",")
            .append("\"category\":\"").append(escape(quest.metadata().category())).append("\",")
            .append("\"type\":\"").append(escape(quest.metadata().type().name())).append("\",")
            .append("\"difficulty\":\"").append(escape(quest.metadata().difficulty().name())).append("\",")
            .append("\"enabled\":").append(quest.enabled()).append(',')
            .append("\"objectives\":").append(quest.objectives().size()).append(',')
            .append("\"requirements\":").append(quest.requirements().size()).append(',')
            .append("\"rewards\":").append(quest.rewards().size())
            .append('}')
            .toString();
    }

    private String npcJson(NpcData npc) {
        return new StringBuilder()
            .append('{')
            .append("\"id\":\"").append(escape(npc.id())).append("\",")
            .append("\"displayName\":\"").append(escape(npc.displayName())).append("\",")
            .append("\"citizensId\":").append(npc.citizensId().isPresent() ? npc.citizensId().getAsInt() : -1).append(',')
            .append("\"skin\":\"").append(escape(npc.skin())).append("\",")
            .append("\"location\":\"").append(escape(npc.location())).append("\",")
            .append("\"quests\":").append(npc.quests().size()).append(',')
            .append("\"routePoints\":").append(npc.routePoints().size())
            .append('}')
            .toString();
    }

    private String playerRankJson(PlayerRankEntry entry) {
        return new StringBuilder()
            .append('{')
            .append("\"playerId\":\"").append(entry.playerId()).append("\",")
            .append("\"completedQuests\":").append(entry.completedQuests()).append(',')
            .append("\"questPoints\":").append(entry.questPoints()).append(',')
            .append("\"reputation\":").append(entry.reputation()).append(',')
            .append("\"achievements\":").append(entry.achievements())
            .append('}')
            .toString();
    }

    private String playerDataJson(PlayerQuestData data) {
        return new StringBuilder()
            .append('{')
            .append("\"playerId\":\"").append(data.playerId()).append("\",")
            .append("\"activeQuests\":").append(data.activeQuests().size()).append(',')
            .append("\"completedQuests\":").append(data.completedQuests().size()).append(',')
            .append("\"questPoints\":").append(data.questPoints()).append(',')
            .append("\"reputation\":").append(data.reputation()).append(',')
            .append("\"achievements\":").append(data.achievements().size()).append(',')
            .append("\"titles\":").append(data.unlockedTitles().size())
            .append('}')
            .toString();
    }

    private String playerDataDetailedJson(PlayerQuestData data) {
        return new StringBuilder()
            .append('{')
            .append("\"playerId\":\"").append(data.playerId()).append("\",")
            .append("\"activeQuests\":").append(questIdArrayJson(data.activeQuests().keySet())).append(',')
            .append("\"completedQuests\":").append(questIdArrayJson(data.completedQuests())).append(',')
            .append("\"trackedQuest\":\"").append(escape(data.trackedQuest().map(QuestId::value).orElse(""))).append("\",")
            .append("\"questPoints\":").append(data.questPoints()).append(',')
            .append("\"reputation\":").append(data.reputation()).append(',')
            .append("\"unlockedTitles\":").append(stringArrayJson(data.unlockedTitles())).append(',')
            .append("\"achievements\":").append(stringArrayJson(data.achievements())).append(',')
            .append("\"selectedTitle\":\"").append(escape(data.selectedTitle().orElse(""))).append("\",")
            .append("\"updatedAt\":").append(data.updatedAt())
            .append('}')
            .toString();
    }

    private String playerDataArrayJson(Collection<PlayerQuestData> players) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (PlayerQuestData data : players.stream().sorted(java.util.Comparator.comparing(PlayerQuestData::playerId)).toList()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append(playerDataDetailedJson(data));
        }
        json.append(']');
        return json.toString();
    }

    private String titleStatsJson(List<PlayerQuestData> players) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PlayerQuestData data : players) {
            for (String title : data.unlockedTitles()) {
                counts.merge(title, 1, Integer::sum);
            }
        }
        return countedStringJson("titles", counts);
    }

    private String achievementStatsJson(List<PlayerQuestData> players) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (PlayerQuestData data : players) {
            for (String achievement : data.achievements()) {
                counts.merge(achievement, 1, Integer::sum);
            }
        }
        return countedStringJson("achievements", counts);
    }

    private String countedStringJson(String rootName, Map<String, Integer> counts) {
        StringBuilder json = new StringBuilder("{\"").append(rootName).append("\":[");
        boolean first = true;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('{')
                .append("\"id\":\"").append(escape(entry.getKey())).append("\",")
                .append("\"players\":").append(entry.getValue())
                .append('}');
        }
        json.append("]}");
        return json.toString();
    }

    private String advancementSnapshotJson() {
        Map<String, Boolean> snapshot = plugin.services().mainThread()
            .supply(() -> plugin.services().advancements().loadedSnapshot())
            .join();
        StringBuilder json = new StringBuilder("{\"advancements\":[");
        boolean first = true;
        for (Map.Entry<String, Boolean> entry : snapshot.entrySet()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('{')
                .append("\"id\":\"").append(escape(entry.getKey())).append("\",")
                .append("\"loaded\":").append(entry.getValue())
                .append('}');
        }
        json.append("]}");
        return json.toString();
    }

    private String questIdArrayJson(Collection<QuestId> values) {
        return stringArrayJson(values.stream().map(QuestId::value).sorted().toList());
    }

    private String stringArrayJson(Collection<String> values) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (String value : values.stream().sorted().toList()) {
            if (!first) {
                json.append(',');
            }
            first = false;
            json.append('"').append(escape(value)).append('"');
        }
        json.append(']');
        return json.toString();
    }

    private Optional<UUID> uuid(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private String suffix(String path, String prefix) {
        if (path.length() <= prefix.length()) {
            return "";
        }
        String suffix = path.substring(prefix.length());
        if (suffix.startsWith("/")) {
            suffix = suffix.substring(1);
        }
        return urlDecode(suffix);
    }

    private String urlDecode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r");
    }

    private void sendJson(HttpExchange exchange, int statusCode, String json) throws IOException {
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(statusCode, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private record RateWindow(long minute, int count) {
    }
}
