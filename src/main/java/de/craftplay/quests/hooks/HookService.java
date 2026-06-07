package de.craftplay.quests.hooks;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Bukkit;

public final class HookService {

    private static final String[] KNOWN_HOOKS = {
        "Citizens",
        "PlaceholderAPI",
        "CMI",
        "Jobs",
        "HeadDatabase",
        "floodgate",
        "LuckPerms",
        "Vault"
    };

    private final CraftplayQuestsPlugin plugin;
    private final Map<String, Boolean> hooks = new LinkedHashMap<>();

    public HookService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void detect() {
        hooks.clear();
        for (String hook : KNOWN_HOOKS) {
            boolean enabled = Bukkit.getPluginManager().isPluginEnabled(hook);
            hooks.put(hook.toLowerCase(), enabled);
            plugin.getLogger().info("Hook " + hook + ": " + (enabled ? "enabled" : "not present"));
        }
    }

    public boolean enabled(String hook) {
        return hooks.getOrDefault(hook.toLowerCase(), false);
    }

    public Map<String, Boolean> snapshot() {
        return Map.copyOf(hooks);
    }
}
