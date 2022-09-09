package concurrencytest.runtime;

import concurrencytest.checkpoint.Checkpoint;

public class CheckpointReached {

    private final Checkpoint checkpoint;
    private final Object details;
    private final Thread thread;

    public CheckpointReached(Checkpoint checkpoint, Object details, Thread thread) {
        this.checkpoint = checkpoint;
        this.details = details;
        this.thread = thread;
    }

    public Thread getThread() {
        return thread;
    }

    public Object getDetails() {
        return details;
    }

    public Checkpoint getCheckpoint() {
        return checkpoint;
    }
}
