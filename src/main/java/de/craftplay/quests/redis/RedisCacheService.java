package de.craftplay.quests.redis;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public final class RedisCacheService {

    private final CraftplayQuestsPlugin plugin;
    private volatile boolean available;
    private Class<?> jedisClass;
    private Constructor<?> jedisConstructor;
    private Method authMethod;
    private Method selectMethod;
    private Method getMethod;
    private Method setMethod;
    private Method delMethod;
    private Method publishMethod;
    private Method closeMethod;

    public RedisCacheService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> initialize() {
        if (!plugin.getConfig().getBoolean("redis.enabled", false)) {
            available = false;
            return CompletableFuture.completedFuture(null);
        }

        return plugin.services().asyncTasks().runAsync(() -> {
            try {
                ClassLoader classLoader = plugin.services().libraries().libraryClassLoader();
                jedisClass = Class.forName("redis.clients.jedis.Jedis", true, classLoader);
                jedisConstructor = jedisClass.getConstructor(String.class, int.class);
                authMethod = jedisClass.getMethod("auth", String.class);
                selectMethod = jedisClass.getMethod("select", int.class);
                getMethod = jedisClass.getMethod("get", String.class);
                setMethod = jedisClass.getMethod("set", String.class, String.class);
                delMethod = jedisClass.getMethod("del", String.class);
                publishMethod = jedisClass.getMethod("publish", String.class, String.class);
                closeMethod = jedisClass.getMethod("close");
                tryConnection();
                available = true;
                plugin.getLogger().info("Redis cache service enabled.");
            } catch (ReflectiveOperationException exception) {
                available = false;
                throw new IllegalStateException("Could not initialize Redis cache service", exception);
            }
        });
    }

    public CompletableFuture<Void> shutdown() {
        available = false;
        return CompletableFuture.completedFuture(null);
    }

    public boolean available() {
        return available;
    }

    public CompletableFuture<Optional<String>> get(String key) {
        if (!available) {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return plugin.services().asyncTasks().supplyAsync(() -> withConnection(connection -> {
            Object value = getMethod.invoke(connection, namespaced(key));
            return Optional.ofNullable(value).map(String::valueOf);
        }));
    }

    public CompletableFuture<Void> set(String key, String value) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return plugin.services().asyncTasks().runAsync(() -> withConnection(connection -> {
            setMethod.invoke(connection, namespaced(key), value);
            return null;
        }));
    }

    public CompletableFuture<Void> delete(String key) {
        if (!available) {
            return CompletableFuture.completedFuture(null);
        }
        return plugin.services().asyncTasks().runAsync(() -> withConnection(connection -> {
            delMethod.invoke(connection, namespaced(key));
            return null;
        }));
    }

    public CompletableFuture<Void> publish(String channel, String payload) {
        if (!available || !plugin.getConfig().getBoolean("redis.use-for.network-sync", true)) {
            return CompletableFuture.completedFuture(null);
        }
        return plugin.services().asyncTasks().runAsync(() -> withConnection(connection -> {
            publishMethod.invoke(connection, namespaced(channel), payload);
            return null;
        }));
    }

    private void tryConnection() {
        withConnection(connection -> null);
    }

    private Object newConnection() throws ReflectiveOperationException {
        String host = plugin.getConfig().getString("redis.host", "localhost");
        int port = plugin.getConfig().getInt("redis.port", 6379);
        Object connection = jedisConstructor.newInstance(host, port);
        String password = plugin.getConfig().getString("redis.password", "");
        if (password != null && !password.isBlank()) {
            authMethod.invoke(connection, password);
        }
        int database = plugin.getConfig().getInt("redis.database", 0);
        if (database > 0) {
            selectMethod.invoke(connection, database);
        }
        return connection;
    }

    private <T> T withConnection(RedisOperation<T> operation) {
        Object connection = null;
        try {
            connection = newConnection();
            return operation.run(connection);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Redis operation failed", exception);
        } finally {
            if (connection != null) {
                try {
                    closeMethod.invoke(connection);
                } catch (ReflectiveOperationException ignored) {
                    // Connection is best-effort closed; Redis operations remain independent.
                }
            }
        }
    }

    private String namespaced(String key) {
        return "cpq:" + plugin.serverSettings().serverId() + ":" + key;
    }

    @FunctionalInterface
    private interface RedisOperation<T> {
        T run(Object connection) throws ReflectiveOperationException;
    }
}
