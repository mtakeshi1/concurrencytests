package concurrencytest.runtime;

import concurrencytest.runtime.checkpoint.CheckpointReached;

import java.util.concurrent.Semaphore;

public class CheckpointWithSemaphore {
    private final CheckpointReached checkpointReached;
    private final Thread thread;
    private final Semaphore semaphore = new Semaphore(0);

    public CheckpointWithSemaphore(CheckpointReached checkpointReached, Thread thread) {
        this.checkpointReached = checkpointReached;
        this.thread = thread;
    }

    public CheckpointReached getCheckpointReached() {
        return checkpointReached;
    }

    public Thread getThread() {
        return thread;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public void releaseThread() {
        semaphore.release();
    }

}
