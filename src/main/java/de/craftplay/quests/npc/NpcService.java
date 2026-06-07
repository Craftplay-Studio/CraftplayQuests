package de.craftplay.quests.npc;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.model.QuestId;
import de.craftplay.quests.storage.StorageDocumentKey;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bukkit.Location;
import org.bukkit.configuration.file.YamlConfiguration;

public final class NpcService {

    private static final String NPC_NAMESPACE = "npcs";

    private final CraftplayQuestsPlugin plugin;
    private final Map<String, NpcData> npcs = new ConcurrentHashMap<>();

    public NpcService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public CompletableFuture<Void> initialize() {
        return plugin.services().storage().listDocuments(NPC_NAMESPACE).thenCompose(keys -> {
            List<CompletableFuture<Optional<NpcData>>> futures = keys.stream()
                .map(this::loadNpc)
                .toList();
            return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).thenRun(() -> {
                Map<String, NpcData> loaded = new LinkedHashMap<>();
                for (CompletableFuture<Optional<NpcData>> future : futures) {
                    future.join().ifPresent(npc -> loaded.put(npc.id(), npc));
                }
                npcs.clear();
                npcs.putAll(loaded);
                plugin.getLogger().info("Loaded " + loaded.size() + " CraftplayQuests NPCs.");
            });
        });
    }

    public boolean citizensAvailable() {
        return plugin.services().hooks().enabled("Citizens");
    }

    public CompletableFuture<NpcData> create(String id, String displayName) {
        NpcData npc = new NpcData(id, OptionalInt.empty(), displayName, "", "", List.of(), List.of());
        return save(npc).thenApply(ignored -> npc);
    }

    public CompletableFuture<Boolean> delete(String id) {
        String normalized = normalize(id);
        npcs.remove(normalized);
        return plugin.services().storage().deleteDocument(new StorageDocumentKey(NPC_NAMESPACE, normalized));
    }

    public CompletableFuture<NpcData> linkCitizens(String cpqId, int citizensId) {
        NpcData current = npcs.getOrDefault(normalize(cpqId), new NpcData(cpqId, OptionalInt.empty(), cpqId, "", "", List.of(), List.of()));
        NpcData updated = new NpcData(current.id(), OptionalInt.of(citizensId), current.displayName(), current.skin(), current.location(), current.quests(), current.routePoints());
        return save(updated).thenApply(ignored -> updated);
    }

    public CompletableFuture<NpcData> setSkin(String cpqId, String skin) {
        NpcData current = required(cpqId);
        NpcData updated = new NpcData(current.id(), current.citizensId(), current.displayName(), skin, current.location(), current.quests(), current.routePoints());
        return save(updated).thenApply(ignored -> updated);
    }

    public CompletableFuture<NpcData> setLocation(String cpqId, Location location) {
        NpcData current = required(cpqId);
        String serialized = location.getWorld().getName() + ","
            + location.getX() + "," + location.getY() + "," + location.getZ() + ","
            + location.getYaw() + "," + location.getPitch();
        NpcData updated = new NpcData(current.id(), current.citizensId(), current.displayName(), current.skin(), serialized, current.quests(), current.routePoints());
        return save(updated).thenApply(ignored -> updated);
    }

    public CompletableFuture<NpcData> addQuest(String cpqId, QuestId questId) {
        NpcData current = required(cpqId);
        java.util.ArrayList<QuestId> quests = new java.util.ArrayList<>(current.quests());
        if (!quests.contains(questId)) {
            quests.add(questId);
        }
        NpcData updated = new NpcData(current.id(), current.citizensId(), current.displayName(), current.skin(), current.location(), quests, current.routePoints());
        return save(updated).thenApply(ignored -> updated);
    }

    public CompletableFuture<NpcData> removeQuest(String cpqId, QuestId questId) {
        NpcData current = required(cpqId);
        java.util.ArrayList<QuestId> quests = new java.util.ArrayList<>(current.quests());
        quests.remove(questId);
        NpcData updated = new NpcData(current.id(), current.citizensId(), current.displayName(), current.skin(), current.location(), quests, current.routePoints());
        return save(updated).thenApply(ignored -> updated);
    }

    public CompletableFuture<NpcData> addRoutePoint(String cpqId, Location location) {
        NpcData current = required(cpqId);
        java.util.ArrayList<String> points = new java.util.ArrayList<>(current.routePoints());
        points.add(location.getWorld().getName() + "," + location.getX() + "," + location.getY() + "," + location.getZ());
        NpcData updated = new NpcData(current.id(), current.citizensId(), current.displayName(), current.skin(), current.location(), current.quests(), points);
        return save(updated).thenApply(ignored -> updated);
    }

    public Optional<NpcData> find(String id) {
        return Optional.ofNullable(npcs.get(normalize(id)));
    }

    public List<NpcData> all() {
        return npcs.values().stream()
            .sorted(java.util.Comparator.comparing(NpcData::id))
            .toList();
    }

    public Map<String, Integer> linkedNpcs() {
        return npcs.values().stream()
            .filter(npc -> npc.citizensId().isPresent())
            .collect(Collectors.toUnmodifiableMap(NpcData::id, npc -> npc.citizensId().getAsInt()));
    }

    private CompletableFuture<Optional<NpcData>> loadNpc(String key) {
        return plugin.services().storage().loadDocument(new StorageDocumentKey(NPC_NAMESPACE, key))
            .thenApply(document -> document.map(this::deserialize))
            .handle((npc, throwable) -> {
                if (throwable == null) {
                    return npc;
                }
                plugin.getLogger().warning("Could not load NPC " + key + ": " + throwable.getMessage());
                return Optional.empty();
            });
    }

    private CompletableFuture<Void> save(NpcData npc) {
        npcs.put(npc.id(), npc);
        return plugin.services().storage().saveDocument(new StorageDocumentKey(NPC_NAMESPACE, npc.id()), serialize(npc));
    }

    private NpcData required(String id) {
        return find(id).orElseThrow(() -> new IllegalArgumentException("NPC not found: " + id));
    }

    private String serialize(NpcData npc) {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("id", npc.id());
        npc.citizensId().ifPresent(value -> configuration.set("citizens-id", value));
        configuration.set("display-name", npc.displayName());
        configuration.set("skin", npc.skin());
        configuration.set("location", npc.location());
        configuration.set("quests", npc.quests().stream().map(QuestId::value).toList());
        configuration.set("route-points", npc.routePoints());
        return configuration.saveToString();
    }

    private NpcData deserialize(String content) {
        YamlConfiguration configuration = new YamlConfiguration();
        try {
            configuration.loadFromString(content);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Invalid NPC YAML", exception);
        }
        int citizensId = configuration.getInt("citizens-id", -1);
        return new NpcData(
            configuration.getString("id", "npc"),
            citizensId >= 0 ? OptionalInt.of(citizensId) : OptionalInt.empty(),
            configuration.getString("display-name", "NPC"),
            configuration.getString("skin", ""),
            configuration.getString("location", ""),
            configuration.getStringList("quests").stream().map(QuestId::of).toList(),
            configuration.getStringList("route-points")
        );
    }

    private String normalize(String id) {
        return id == null ? "" : id.trim().toLowerCase(java.util.Locale.ROOT);
    }
}
