package de.craftplay.quests.security;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class ConfirmService {

    private static final char[] ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();

    private final CraftplayQuestsPlugin plugin;
    private final SecureRandom random = new SecureRandom();
    private final ConcurrentHashMap<String, PendingConfirmation> confirmations = new ConcurrentHashMap<>();

    public ConfirmService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public PendingConfirmation request(CommandSender sender, String action, String detail) {
        String actor = actorKey(sender);
        confirmations.entrySet().removeIf(entry -> entry.getValue().expired());
        confirmations.entrySet().removeIf(entry -> entry.getValue().actor().equals(actor));

        int length = Math.max(4, plugin.getConfig().getInt("confirm.code-length", 8));
        long expiresAt = System.currentTimeMillis() + Math.max(5, plugin.getConfig().getInt("confirm.expire-seconds", 60)) * 1000L;
        String code = code(length);
        PendingConfirmation confirmation = new PendingConfirmation(actor, action, detail, code, expiresAt);
        confirmations.put(code.toUpperCase(Locale.ROOT), confirmation);
        return confirmation;
    }

    public Optional<PendingConfirmation> confirm(CommandSender sender, String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }

        String normalized = code.toUpperCase(Locale.ROOT);
        PendingConfirmation confirmation = confirmations.remove(normalized);
        if (confirmation == null || confirmation.expired() || !confirmation.actor().equals(actorKey(sender))) {
            return Optional.empty();
        }
        return Optional.of(confirmation);
    }

    public boolean required(String path) {
        return plugin.getConfig().getBoolean("confirm.enabled", true)
            && plugin.getConfig().getBoolean("confirm.require-for." + path, true);
    }

    private String actorKey(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.getUniqueId().toString();
        }
        return "console:" + sender.getName().toLowerCase(Locale.ROOT);
    }

    private String code(int length) {
        StringBuilder result = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            result.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return result.toString();
    }

    public record PendingConfirmation(String actor, String action, String detail, String code, long expiresAt) {

        public boolean expired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
