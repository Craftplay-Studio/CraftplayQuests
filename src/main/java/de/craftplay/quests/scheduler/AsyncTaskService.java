package de.craftplay.quests.scheduler;

import de.craftplay.quests.CraftplayQuestsPlugin;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public final class AsyncTaskService implements AutoCloseable {

    private final CraftplayQuestsPlugin plugin;
    private final MainThreadService mainThreadService;
    private final ExecutorService executorService;

    public AsyncTaskService(CraftplayQuestsPlugin plugin, MainThreadService mainThreadService, int workers) {
        this.plugin = plugin;
        this.mainThreadService = mainThreadService;
        this.executorService = Executors.newFixedThreadPool(Math.max(1, workers), new WorkerThreadFactory());
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable");
        return CompletableFuture.runAsync(wrap(runnable), executorService);
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        Objects.requireNonNull(supplier, "supplier");
        return CompletableFuture.supplyAsync(wrap(supplier), executorService);
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier, TaskCallback<T> callback) {
        CompletableFuture<T> future = supplyAsync(supplier);
        future.whenComplete((value, throwable) -> mainThreadService.execute(() -> {
            if (throwable != null) {
                callback.complete(TaskResult.failure(throwable));
                return;
            }
            callback.complete(TaskResult.success(value));
        }));
        return future;
    }

    public CompletableFuture<Void> runAsync(Runnable runnable, TaskCallback<Void> callback) {
        CompletableFuture<Void> future = runAsync(runnable);
        future.whenComplete((value, throwable) -> mainThreadService.execute(() -> {
            if (throwable != null) {
                callback.complete(TaskResult.failure(throwable));
                return;
            }
            callback.complete(TaskResult.success(null));
        }));
        return future;
    }

    public void shutdown(Duration timeout) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }

    @Override
    public void close() {
        shutdown(Duration.ofSeconds(10));
    }

    private Runnable wrap(Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (RuntimeException exception) {
                plugin.getLogger().severe("Async task failed: " + exception.getMessage());
                throw exception;
            }
        };
    }

    private <T> Supplier<T> wrap(Supplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (RuntimeException exception) {
                plugin.getLogger().severe("Async task failed: " + exception.getMessage());
                throw exception;
            }
        };
    }

    private static final class WorkerThreadFactory implements ThreadFactory {

        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "CraftplayQuests-Worker-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
