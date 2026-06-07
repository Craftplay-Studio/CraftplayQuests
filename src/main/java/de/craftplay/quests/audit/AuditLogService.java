package de.craftplay.quests.audit;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import org.bukkit.command.CommandSender;

public final class AuditLogService {

    private final CraftplayQuestsPlugin plugin;

    public AuditLogService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void record(CommandSender actor, String action, String detail) {
        record(actor == null ? "system" : actor.getName(), action, detail);
    }

    public void record(String actor, String action, String detail) {
        plugin.services().asyncTasks().runAsync(() -> {
            try {
                Path logFile = plugin.getDataFolder().toPath().resolve("logs").resolve("audit.log").normalize();
                Files.createDirectories(logFile.getParent());
                String line = Instant.now() + "\t" + clean(actor) + "\t" + clean(action) + "\t" + clean(detail) + System.lineSeparator();
                Files.writeString(
                    logFile,
                    line,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
                );
            } catch (Exception exception) {
                plugin.getLogger().warning("Could not write audit log: " + exception.getMessage());
            }
        });
    }

    private String clean(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\n', ' ').replace('\r', ' ').replace('\t', ' ');
    }
}
