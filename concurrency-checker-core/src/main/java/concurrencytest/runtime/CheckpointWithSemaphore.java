package concurrencytest.runtime;

import concurrencytest.runtime.checkpoint.CheckpointReached;

import java.util.concurrent.Future;
import java.util.concurrent.Semaphore;

public class CheckpointWithSemaphore {
    private final CheckpointReached checkpointReached;
    private final Future<?> future;
    private final Semaphore semaphore = new Semaphore(0);

    public CheckpointWithSemaphore(CheckpointReached checkpointReached, Future<?> future) {
        this.checkpointReached = checkpointReached;
        this.future = future;
    }

    public CheckpointReached getCheckpointReached() {
        return checkpointReached;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public void releaseThread() {
        semaphore.release();
    }

    public Future<?> getFuture() {
        return future;
    }
}
