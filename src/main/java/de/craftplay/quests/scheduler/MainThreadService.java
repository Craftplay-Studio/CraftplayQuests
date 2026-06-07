package de.craftplay.quests.scheduler;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public final class MainThreadService {

    private final CraftplayQuestsPlugin plugin;

    public MainThreadService(CraftplayQuestsPlugin plugin) {
        this.plugin = plugin;
    }

    public void execute(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public BukkitTask executeLater(Runnable runnable, long delayTicks) {
        Objects.requireNonNull(runnable, "runnable");
        return Bukkit.getScheduler().runTaskLater(plugin, runnable, Math.max(0L, delayTicks));
    }

    public <T> CompletableFuture<T> supply(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        CompletableFuture<T> future = new CompletableFuture<>();
        execute(() -> {
            try {
                future.complete(supplier.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        });
        return future;
    }

    public void ensureMainThread(String operation) {
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException(operation + " must run on the Bukkit main thread.");
        }
    }
}
