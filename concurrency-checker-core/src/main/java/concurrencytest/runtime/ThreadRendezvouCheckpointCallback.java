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
    public synchronized void checkpointReached(ManagedThread managedThread, CheckpointReached checkpointReached, RuntimeState currentState) throws Exception {
        CheckpointWithSemaphore old = threadCheckpoints.put(managedThread.getActorName(), new CheckpointWithSemaphore(checkpointReached, managedThread));
        if (old != null) {
            throw new IllegalStateException("actor %s was already at a checkpoint? Previous checkpoint: %s, current checkpoint: %s".formatted(managedThread.getActorName(), old.getCheckpointReached().checkpoint(), checkpointReached.checkpoint()));
        }
        notifyAll();
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
}
