package de.craftplay.quests.library;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.scheduler.AsyncTaskService;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class LibraryLoaderService {

    private static final Map<String, LibraryDefinition> DEFAULT_LIBRARIES = defaultLibraries();

    private final CraftplayQuestsPlugin plugin;
    private final AsyncTaskService asyncTaskService;
    private final HttpClient httpClient;
    private final Set<String> registeredDrivers = ConcurrentHashMap.newKeySet();
    private volatile URLClassLoader cachedClassLoader;

    public LibraryLoaderService(CraftplayQuestsPlugin plugin, AsyncTaskService asyncTaskService) {
        this.plugin = plugin;
        this.asyncTaskService = asyncTaskService;
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    public CompletableFuture<LibraryPreparationReport> prepareConfiguredLibraries() {
        return asyncTaskService.supplyAsync(this::prepareConfiguredLibrariesBlocking);
    }

    public Optional<Path> resolveLibraryJar(String key) {
        LibraryDefinition definition = libraryDefinitions().get(key.toLowerCase());
        if (definition == null) {
            return Optional.empty();
        }

        Path jar = cacheFolder().resolve(definition.fileName());
        if (Files.exists(jar)) {
            return Optional.of(jar);
        }
        return Optional.empty();
    }

    public ClassLoader libraryClassLoader() {
        URLClassLoader existing = cachedClassLoader;
        if (existing != null) {
            return existing;
        }

        synchronized (this) {
            if (cachedClassLoader != null) {
                return cachedClassLoader;
            }

            try {
                Files.createDirectories(cacheFolder());
                List<URL> urls = new ArrayList<>();
                try (var stream = Files.list(cacheFolder())) {
                    for (Path path : stream.filter(file -> file.toString().endsWith(".jar")).toList()) {
                        urls.add(path.toUri().toURL());
                    }
                }
                cachedClassLoader = new URLClassLoader(urls.toArray(URL[]::new), plugin.getClass().getClassLoader());
                return cachedClassLoader;
            } catch (IOException exception) {
                throw new IllegalStateException("Could not create library classloader", exception);
            }
        }
    }

    public void registerJdbcDriver(String key) {
        LibraryDefinition definition = libraryDefinitions().get(key.toLowerCase());
        if (definition == null || definition.driverClass() == null || definition.driverClass().isBlank()) {
            throw new IllegalArgumentException("No JDBC driver definition for " + key);
        }

        if (!registeredDrivers.add(definition.key())) {
            return;
        }

        try {
            Class<?> driverType = Class.forName(definition.driverClass(), true, libraryClassLoader());
            Driver driver = (Driver) driverType.getDeclaredConstructor().newInstance();
            DriverManager.registerDriver(new DriverShim(driver));
        } catch (ReflectiveOperationException | SQLException exception) {
            registeredDrivers.remove(definition.key());
            throw new IllegalStateException("Could not register JDBC driver " + definition.driverClass(), exception);
        }
    }

    private LibraryPreparationReport prepareConfiguredLibrariesBlocking() {
        boolean autoDownload = plugin.getConfig().getBoolean("libraries.auto-download", true);
        List<String> repositories = plugin.getConfig().getStringList("libraries.repositories");
        if (repositories.isEmpty()) {
            repositories = List.of("https://repo1.maven.org/maven2/");
        }

        List<Path> cached = new ArrayList<>();
        List<Path> downloaded = new ArrayList<>();
        List<LibraryDefinition> missing = new ArrayList<>();
        List<LibraryDefinition> skipped = new ArrayList<>();

        try {
            Files.createDirectories(cacheFolder());
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create library cache folder", exception);
        }

        for (LibraryDefinition definition : libraryDefinitions().values()) {
            if (!plugin.getConfig().getBoolean("libraries.required." + definition.key(), false)) {
                skipped.add(definition);
                continue;
            }

            Path jar = cacheFolder().resolve(definition.fileName());
            if (Files.exists(jar)) {
                cached.add(jar);
                continue;
            }

            if (!autoDownload) {
                missing.add(definition);
                continue;
            }

            try {
                downloaded.add(download(definition, repositories, jar));
            } catch (IOException | InterruptedException exception) {
                if (exception instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                plugin.getLogger().warning("Could not download library " + definition.key() + ": " + exception.getMessage());
                missing.add(definition);
            }
        }

        cachedClassLoader = null;
        return new LibraryPreparationReport(List.copyOf(cached), List.copyOf(downloaded), List.copyOf(missing), List.copyOf(skipped));
    }

    private Path download(LibraryDefinition definition, List<String> repositories, Path target) throws IOException, InterruptedException {
        IOException lastException = null;
        for (String repository : repositories) {
            String base = repository.endsWith("/") ? repository : repository + "/";
            URI uri = URI.create(base + definition.repositoryPath());
            HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();
            HttpResponse<Path> response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(tempFile(definition)));

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                Files.move(response.body(), target, StandardCopyOption.REPLACE_EXISTING);
                return target;
            }

            Files.deleteIfExists(response.body());
            lastException = new IOException("HTTP " + response.statusCode() + " from " + uri);
        }

        throw lastException == null ? new IOException("No repository configured") : lastException;
    }

    private Path tempFile(LibraryDefinition definition) throws IOException {
        Path tempFolder = plugin.getDataFolder().toPath().resolve("temp").normalize();
        Files.createDirectories(tempFolder);
        return Files.createTempFile(tempFolder, definition.key() + "-", ".jar.tmp");
    }

    private Path cacheFolder() {
        String configured = plugin.getConfig().getString("libraries.cache-folder", "lib");
        Path dataFolder = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
        Path folder = dataFolder.resolve(configured).normalize();
        if (!folder.startsWith(dataFolder)) {
            throw new IllegalArgumentException("Library cache folder must stay inside the plugin data folder.");
        }
        return folder;
    }

    private Map<String, LibraryDefinition> libraryDefinitions() {
        Map<String, LibraryDefinition> definitions = new LinkedHashMap<>();
        for (LibraryDefinition defaults : DEFAULT_LIBRARIES.values()) {
            String path = "libraries.artifacts." + defaults.key();
            definitions.put(defaults.key(), new LibraryDefinition(
                defaults.key(),
                plugin.getConfig().getString(path + ".group-id", defaults.groupId()),
                plugin.getConfig().getString(path + ".artifact-id", defaults.artifactId()),
                plugin.getConfig().getString(path + ".version", defaults.version()),
                plugin.getConfig().getString(path + ".driver-class", defaults.driverClass())
            ));
        }
        return definitions;
    }

    private static Map<String, LibraryDefinition> defaultLibraries() {
        Map<String, LibraryDefinition> libraries = new LinkedHashMap<>();
        libraries.put("mysql", new LibraryDefinition(
            "mysql",
            "com.mysql",
            "mysql-connector-j",
            "9.7.0",
            "com.mysql.cj.jdbc.Driver"
        ));
        libraries.put("mariadb", new LibraryDefinition(
            "mariadb",
            "org.mariadb.jdbc",
            "mariadb-java-client",
            "3.5.8",
            "org.mariadb.jdbc.Driver"
        ));
        libraries.put("h2", new LibraryDefinition(
            "h2",
            "com.h2database",
            "h2",
            "2.4.240",
            "org.h2.Driver"
        ));
        libraries.put("redis", new LibraryDefinition(
            "redis",
            "redis.clients",
            "jedis",
            "7.5.2",
            null
        ));
        return Map.copyOf(libraries);
    }
}
