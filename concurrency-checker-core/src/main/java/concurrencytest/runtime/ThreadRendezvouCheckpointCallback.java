package concurrencytest.runtime;

import concurrencytest.runner.CheckpointReachedCallback;
import concurrencytest.runtime.checkpoint.CheckpointReached;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ThreadRendezvouCheckpointCallback implements CheckpointReachedCallback {

    private final Map<String, CheckpointWithSemaphore> threadCheckpoints;

    public ThreadRendezvouCheckpointCallback(Map<String, CheckpointWithSemaphore> threadCheckpoints) {
        this.threadCheckpoints = new ConcurrentHashMap<>(threadCheckpoints);
    }

    public ThreadRendezvouCheckpointCallback() {
        this(new ConcurrentHashMap<>());
    }

    @Override
    public void checkpointReached(CheckpointReached checkpointReached) throws Exception {
        CheckpointWithSemaphore sem;
        synchronized (this) {
            sem = new CheckpointWithSemaphore(checkpointReached, checkpointReached.thread());
            CheckpointWithSemaphore old = threadCheckpoints.put(checkpointReached.actorName(), sem);
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
        Objects.requireNonNull(semaphore).getSemaphore().release();
    }

    public void actorFinished(String actorName, Duration maxWait) throws InterruptedException {
        CheckpointWithSemaphore semaphore = threadCheckpoints.remove(actorName);
        Objects.requireNonNull(semaphore).getSemaphore().release();
        semaphore.getThread().join(maxWait.toMillis());
    }
}
