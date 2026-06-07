package de.craftplay.quests.title;

import de.craftplay.quests.CraftplayQuestsPlugin;
import de.craftplay.quests.quest.player.PlayerQuestData;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitTask;

public final class TitleService implements Listener {

    private final CraftplayQuestsPlugin plugin;
    private final Map<UUID, TextDisplay> displays = new ConcurrentHashMap<>();
    private BukkitTask updateTask;

    public TitleService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        if (!textDisplaysEnabled() || updateTask != null) {
            return;
        }
        long interval = Math.max(1L, plugin.getConfig().getLong("titles.update-interval-ticks", 10L));
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, interval, interval);
    }

    public void shutdown() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        for (TextDisplay display : displays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        displays.clear();
    }

    public Optional<String> selectedTitle(PlayerQuestData data) {
        if (plugin.getConfig().getBoolean("titles.only-show-if-title-selected", true)) {
            return data.selectedTitle();
        }
        return data.selectedTitle().or(() -> data.unlockedTitles().stream().findFirst());
    }

    public CompletableFuture<PlayerQuestData> selectTitle(UUID playerId, String title) {
        return plugin.services().quests().playerData(playerId)
            .thenCompose(data -> {
                PlayerQuestData updated = data.withSelectedTitle(Optional.of(title));
                return plugin.services().quests().savePlayerData(updated);
            });
    }

    public CompletableFuture<PlayerQuestData> grantTitle(UUID playerId, String title) {
        return plugin.services().quests().playerData(playerId)
            .thenCompose(data -> plugin.services().quests().savePlayerData(data.unlockTitle(title)));
    }

    public CompletableFuture<PlayerQuestData> clearTitle(UUID playerId) {
        return plugin.services().quests().playerData(playerId)
            .thenCompose(data -> plugin.services().quests().savePlayerData(data.withSelectedTitle(Optional.empty())));
    }

    public boolean textDisplaysEnabled() {
        return plugin.getConfig().getBoolean("titles.enabled", true) && plugin.services().version().supportsTextDisplay();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        remove(event.getPlayer().getUniqueId());
    }

    private void tick() {
        if (!textDisplaysEnabled()) {
            shutdown();
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            update(player);
        }
        displays.keySet().removeIf(playerId -> {
            if (Bukkit.getPlayer(playerId) != null) {
                return false;
            }
            remove(playerId);
            return true;
        });
    }

    private void update(Player player) {
        Optional<PlayerQuestData> cached = plugin.services().quests().cachedPlayerData(player.getUniqueId());
        if (cached.isEmpty()) {
            plugin.services().quests().playerData(player.getUniqueId());
            remove(player.getUniqueId());
            return;
        }

        Optional<String> title = selectedTitle(cached.get());
        if (title.isEmpty() || hidden(player)) {
            remove(player.getUniqueId());
            return;
        }

        TextDisplay display = displays.compute(player.getUniqueId(), (ignored, current) -> {
            if (current != null && current.isValid() && current.getWorld().equals(player.getWorld())) {
                return current;
            }
            if (current != null && current.isValid()) {
                current.remove();
            }
            return spawn(player);
        });

        if (display == null || !display.isValid()) {
            return;
        }

        Location target = player.getLocation().add(0.0D, plugin.getConfig().getDouble("titles.height-offset", 2.45D), 0.0D);
        display.teleport(target);
        display.text(titleComponent(title.get()));
        updateViewDistance(player, display);
    }

    private TextDisplay spawn(Player player) {
        Location location = player.getLocation().add(0.0D, plugin.getConfig().getDouble("titles.height-offset", 2.45D), 0.0D);
        return player.getWorld().spawn(location, TextDisplay.class, display -> {
            display.setPersistent(false);
            display.setInvulnerable(true);
            display.setGravity(false);
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setAlignment(TextDisplay.TextAlignment.CENTER);
        });
    }

    private Component titleComponent(String title) {
        return plugin.language().message("title.display-format", Map.of("title", title));
    }

    private boolean hidden(Player player) {
        if (plugin.getConfig().getBoolean("titles.hide-while-sneaking", true) && player.isSneaking()) {
            return true;
        }
        if (!plugin.getConfig().getBoolean("titles.hide-in-vanish", true)) {
            return false;
        }
        return player.hasMetadata("vanished")
            || player.hasMetadata("invisible")
            || player.hasMetadata("cmi vanished")
            || player.hasMetadata("CMI-Vanish");
    }

    private void updateViewDistance(Player owner, Entity display) {
        double maxDistanceSquared = Math.pow(plugin.getConfig().getDouble("titles.view-distance", 32.0D), 2.0D);
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (!viewer.getWorld().equals(owner.getWorld()) || viewer.getLocation().distanceSquared(owner.getLocation()) > maxDistanceSquared) {
                viewer.hideEntity(plugin, display);
            } else {
                viewer.showEntity(plugin, display);
            }
        }
    }

    private void remove(UUID playerId) {
        TextDisplay display = displays.remove(playerId);
        if (display != null && display.isValid()) {
            display.remove();
        }
    }
}
