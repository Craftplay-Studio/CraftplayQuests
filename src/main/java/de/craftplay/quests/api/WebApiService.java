package de.craftplay.quests.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.npc.NpcData;
import de.craftplay.quests.quest.model.Quest;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.quest.player.PlayerQuestData;
import de.craftplay.quests.quest.storage.QuestSerializer;
import de.craftplay.quests.stats.PlayerRankEntry;
import de.craftplay.quests.stats.StatsSnapshot;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class WebApiService {

    private final CraftplayQuestsPlugin plugin;
    private final QuestSerializer questSerializer = new QuestSerializer();
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
            createdServer.createContext("/api/admin/import", this::handleImport);
            createdServer.createContext("/api/admin/cache", this::handleCache);
            createdServer.createContext("/api/admin/export", this::handleExport);
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
        if (!plugin.getConfig().getBoolean("api.allow-health-without-token", true) && !authorized(exchange)) {
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
        if (!authorized(exchange)) {
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
        if (!authorized(exchange)) {
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
        if (!authorized(exchange)) {
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
        if (!authorized(exchange)) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }
        String playerId = suffix(exchange.getRequestURI().getPath(), "/api/stats/player");
        if (playerId.isBlank()) {
            sendJson(exchange, 400, "{\"error\":\"player-required\"}");
            return;
        }
        PlayerQuestData data = plugin.services().stats().player(UUID.fromString(playerId)).join();
        sendJson(exchange, 200, playerDataJson(data));
    }

    private void handleNpcs(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange)) {
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
        sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
    }

    private void handleImport(HttpExchange exchange) throws IOException {
        if (rateLimited(exchange)) {
            sendJson(exchange, 429, "{\"error\":\"rate-limited\"}");
            return;
        }
        if (!authorized(exchange)) {
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
        if (!authorized(exchange)) {
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
        if (!authorized(exchange)) {
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

    private boolean authorized(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return plugin.serverSettings().hasApiToken(authorization.substring(7).trim());
        }

        Map<String, String> query = query(exchange);
        return plugin.serverSettings().hasApiToken(query.get("token"));
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
