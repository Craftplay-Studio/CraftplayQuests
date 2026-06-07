package de.craftplay.quests.bedrock;

import de.craftplay.quests.CraftplayQuestsPlugin;
import org.bukkit.entity.Player;

public final class BedrockService {

    private final CraftplayQuestsPlugin plugin;

    public BedrockService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isBedrockPlayer(Player player) {
        if (!plugin.services().hooks().enabled("floodgate")) {
            return false;
        }
        try {
            Class<?> apiClass = Class.forName("org.geysermc.floodgate.api.FloodgateApi");
            Object api = apiClass.getMethod("getInstance").invoke(null);
            Object result = apiClass.getMethod("isFloodgatePlayer", java.util.UUID.class).invoke(api, player.getUniqueId());
            return Boolean.TRUE.equals(result);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}
