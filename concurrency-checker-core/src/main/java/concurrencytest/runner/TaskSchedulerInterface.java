package concurrencytest.runner;

import java.util.Collection;
import java.util.function.Function;

public interface TaskSchedulerInterface {

    interface TaskSpawner {
        boolean spawn(Collection<String> pathPreffix);
    }

    int numberOfRunningTasks();

    int maxRunningTasks();

    default boolean canFork() {
        return numberOfRunningTasks() < maxRunningTasks();
    }

    <E> E spawnTasks(Function<TaskSpawner, E> action);

    void notifyTaskFinished();

}
