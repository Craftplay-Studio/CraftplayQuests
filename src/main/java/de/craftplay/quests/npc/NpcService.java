package de.craftplay.quests.npc;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcService {

    private final CraftplayQuestsPlugin plugin;
    private final Map<String, Integer> linkedCitizensIds = new ConcurrentHashMap<>();

    public NpcService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean citizensAvailable() {
        return plugin.services().hooks().enabled("Citizens");
    }

    public void link(String cpqId, int citizensId) {
        linkedCitizensIds.put(cpqId, citizensId);
    }

    public Map<String, Integer> linkedNpcs() {
        return Map.copyOf(linkedCitizensIds);
    }
}
