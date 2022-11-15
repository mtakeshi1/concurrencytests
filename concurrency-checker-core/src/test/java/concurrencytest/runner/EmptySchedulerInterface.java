package concurrencytest.runner;

import java.util.function.Function;

public class EmptySchedulerInterface implements TaskSchedulerInterface {

    private boolean taskFinished;

    @Override
    public int numberOfRunningTasks() {
        return 1;
    }

    @Override
    public int maxRunningTasks() {
        return 1;
    }

    @Override
    public <E> E spawnTasks(Function<TaskSpawner, E> action) {
        return action.apply(pref -> false);
    }

    @Override
    public synchronized void notifyTaskFinished() {
        taskFinished = true;
        notifyAll();
    }

    public synchronized boolean isTaskFinished() {
        return taskFinished;
    }
}
