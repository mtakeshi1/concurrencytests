package concurrencytest.runtime.impl;

import concurrencytest.runner.CheckpointReachedCallback;
import concurrencytest.runtime.checkpoint.CheckpointReached;
import concurrencytest.runtime.impl.CheckpointWithSemaphore;

import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.*;

public class ThreadRendezvouCheckpointCallback implements CheckpointReachedCallback {

    private final Map<String, CheckpointWithSemaphore> threadCheckpoints;
    private volatile Map<String, Future<Throwable>> actorTasks = new HashMap<>();

    public ThreadRendezvouCheckpointCallback(Map<String, CheckpointWithSemaphore> threadCheckpoints) {
        this.threadCheckpoints = new ConcurrentHashMap<>(threadCheckpoints);
    }

    public ThreadRendezvouCheckpointCallback() {
        this(new ConcurrentHashMap<>());
    }

    @Override
    public void checkpointReached(CheckpointReached checkpointReached) throws Exception {
        // its very likely that the task start checkpoint will not be fully initialized before reaching here,
        // meaning the future can be null. It should not be an issue
        Future<Throwable> future = actorTasks.get(checkpointReached.actorName());
        CheckpointWithSemaphore sem;
        synchronized (this) {
            sem = new CheckpointWithSemaphore(checkpointReached, future);
            CheckpointWithSemaphore old = threadCheckpoints.putIfAbsent(checkpointReached.actorName(), sem);
            if (old != null) {
                throw new IllegalStateException("actor %s was already at a checkpoint? Previous checkpoint: %s, current checkpoint: %s".formatted(checkpointReached.actorName(), old.getCheckpointReached().checkpoint(), checkpointReached.checkpoint()));
            }
            notifyAll();
        }
        sem.getSemaphore().acquire();
    }

    public synchronized void waitForActors(Duration maxWait, Collection<String> expectedActors) throws InterruptedException, TimeoutException {
        long maxTimeNanos = System.nanoTime() + maxWait.toNanos();
        while (!threadCheckpoints.keySet().containsAll(expectedActors)) {
            long rem = TimeUnit.NANOSECONDS.toMillis(maxTimeNanos - System.nanoTime());
            if (rem <= 0) {
                throw new TimeoutException();
            }
            this.wait(rem);
        }

    }

    public CheckpointReached lastKnownCheckpoint(String actorName) {
        return Objects.requireNonNull(threadCheckpoints.get(actorName), "actor %s did not register a checkpoint".formatted(actorName)).getCheckpointReached();
    }

    public void resumeActor(String actorName) {
        CheckpointWithSemaphore semaphore = threadCheckpoints.remove(actorName);
        Objects.requireNonNull(semaphore, "semaphore to control flow of actor '%s' was not found. Existing semaphores: %s".formatted(actorName, threadCheckpoints.keySet())).getSemaphore().release();
    }

    public void actorFinished(String actorName, Duration maxWait) throws InterruptedException, TimeoutException {
        CheckpointWithSemaphore semaphore = threadCheckpoints.remove(actorName);
        Objects.requireNonNull(semaphore).getSemaphore().release();
        try {
            semaphore.getFuture().get(maxWait.toMillis(), TimeUnit.MILLISECONDS);
        } catch (ExecutionException e) {
            //no error is expected at this point, as the inner task should swallow and report the error before reaching here.
            throw new RuntimeException("unexpected error waiting for actor '%s' task to finish: %s".formatted(actorName, e.getCause().getMessage()), e.getCause());
        }
    }

    public void setActorTasks(Map<String, Future<Throwable>> actorTasks) {
        this.actorTasks = actorTasks;
    }

    public void registerNewTask(String newActorName, Future<Throwable> taskMonitor) {
        this.actorTasks.put(newActorName , taskMonitor);
    }
}
