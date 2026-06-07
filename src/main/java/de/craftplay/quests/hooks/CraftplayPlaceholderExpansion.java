package de.craftplay.quests.hooks;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.util.UUID;
import java.util.stream.Collectors;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public final class CraftplayPlaceholderExpansion extends PlaceholderExpansion {

    private final CraftplayQuestsPlugin plugin;

    public CraftplayPlaceholderExpansion(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return plugin.getConfig().getString("placeholderapi.identifier", "cpquests");
    }

    @Override
    public @NotNull String getAuthor() {
        String authors = plugin.getPluginMeta().getAuthors().stream().collect(Collectors.joining(", "));
        return authors.isBlank() ? "Craftplay" : authors;
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return plugin.services().hooks().enabled("PlaceholderAPI");
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        UUID playerId = player == null ? null : player.getUniqueId();
        return plugin.services().placeholders().value(playerId, params);
    }
}
