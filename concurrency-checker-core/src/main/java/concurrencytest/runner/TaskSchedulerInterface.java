package concurrencytest.runner;

import java.util.Collection;
import java.util.function.Consumer;

public interface TaskSchedulerInterface {

    interface TaskSpawner {
        boolean spawn(Collection<String> pathPreffix);
    }

    int numberOfRunningTasks();

    int maxRunningTasks();

    void spawnTasks(Consumer<TaskSpawner> action);


}
