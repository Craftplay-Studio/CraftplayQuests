package de.craftplay.quests.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.Quest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public final class WebApiService {

    private final CraftplayQuestsPlugin plugin;
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
            createdServer.createContext("/api/admin/quests", this::handleQuests);
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
        if (!plugin.getConfig().getBoolean("api.allow-health-without-token", true) && !authorized(exchange)) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }
        sendJson(exchange, 200, "{\"status\":\"ok\",\"plugin\":\"CraftplayQuests\"}");
    }

    private void handleOverview(HttpExchange exchange) throws IOException {
        if (!authorized(exchange)) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
            return;
        }

        StringBuilder json = new StringBuilder();
        json.append('{');
        json.append("\"serverId\":\"").append(escape(plugin.serverSettings().serverId())).append("\",");
        json.append("\"displayName\":\"").append(escape(plugin.serverSettings().displayName())).append("\",");
        json.append("\"version\":\"").append(escape(plugin.getPluginMeta().getVersion())).append("\",");
        json.append("\"storage\":\"").append(escape(plugin.services().storage().activeProviderId())).append("\",");
        json.append("\"minecraftVersion\":\"").append(escape(plugin.services().version().serverVersion())).append("\",");
        json.append("\"versionFamily\":\"").append(escape(plugin.services().version().family().name())).append("\",");
        json.append("\"quests\":").append(plugin.services().quests().registry().size()).append(',');
        json.append("\"apiRunning\":").append(running()).append(',');
        json.append("\"hooks\":").append(hooksJson());
        json.append('}');
        sendJson(exchange, 200, json.toString());
    }

    private void handleQuests(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            sendJson(exchange, 405, "{\"error\":\"method-not-allowed\"}");
            return;
        }
        if (!authorized(exchange)) {
            sendJson(exchange, 401, "{\"error\":\"invalid-token\"}");
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

    private boolean authorized(HttpExchange exchange) {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization != null && authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return plugin.serverSettings().hasApiToken(authorization.substring(7).trim());
        }

        Map<String, String> query = query(exchange);
        return plugin.serverSettings().hasApiToken(query.get("token"));
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
}
