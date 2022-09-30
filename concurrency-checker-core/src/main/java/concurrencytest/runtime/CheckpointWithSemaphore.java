package concurrencytest.runtime;

import concurrencytest.runtime.checkpoint.CheckpointReached;

import java.util.concurrent.Semaphore;

public class CheckpointWithSemaphore {
    private final CheckpointReached checkpointReached;
    private final ManagedThread managedThread;
    private final Semaphore semaphore = new Semaphore(0);

    public CheckpointWithSemaphore(CheckpointReached checkpointReached, ManagedThread managedThread) {
        this.checkpointReached = checkpointReached;
        this.managedThread = managedThread;
    }

    public CheckpointReached getCheckpointReached() {
        return checkpointReached;
    }

    public ManagedThread getManagedThread() {
        return managedThread;
    }

    public Semaphore getSemaphore() {
        return semaphore;
    }

    public void releaseThread() {
        semaphore.release();
    }

}
