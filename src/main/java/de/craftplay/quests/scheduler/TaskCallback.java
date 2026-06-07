package de.craftplay.quests.scheduler;

@FunctionalInterface
public interface TaskCallback<T> {

    void complete(TaskResult<T> result);
}
