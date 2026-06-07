package de.craftplay.quests.scheduler;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public final class TaskResult<T> {

    private final T value;
    private final Throwable error;

    private TaskResult(T value, Throwable error) {
        this.value = value;
        this.error = error;
    }

    public static <T> TaskResult<T> success(T value) {
        return new TaskResult<>(value, null);
    }

    public static <T> TaskResult<T> failure(Throwable error) {
        return new TaskResult<>(null, Objects.requireNonNull(error, "error"));
    }

    public boolean successful() {
        return error == null;
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }

    public T valueOrThrow() {
        if (error != null) {
            if (error instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException(error);
        }
        return value;
    }

    public <R> TaskResult<R> map(Function<T, R> mapper) {
        if (!successful()) {
            return failure(error);
        }
        return success(mapper.apply(value));
    }
}
