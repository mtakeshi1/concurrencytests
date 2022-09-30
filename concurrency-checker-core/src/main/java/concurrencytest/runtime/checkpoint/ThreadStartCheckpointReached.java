package concurrencytest.runtime.checkpoint;

import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.runtime.ManagedThread;

public record ThreadStartCheckpointReached(Checkpoint checkpoint, ManagedThread managedThread, Thread thread) implements CheckpointReached {
    @Override
    public String details() {
        return managedThread.getActorName();
    }

    public String newActorName() {
        return managedThread.getActorName();
    }
}
