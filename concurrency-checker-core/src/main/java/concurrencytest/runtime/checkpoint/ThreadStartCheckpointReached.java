package concurrencytest.runtime.checkpoint;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.runtime.ManagedThread;

public record ThreadStartCheckpointReached(CheckpointDescription checkpoint, ManagedThread managedThread, Thread thread) implements CheckpointReached {
    @Override
    public String details() {
        return managedThread.getActorName();
    }
}
