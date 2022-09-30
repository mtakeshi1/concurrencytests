package concurrencytest.runtime.checkpoint;

import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.runtime.ManagedThread;

public interface CheckpointReached {
    Checkpoint checkpoint();

    String details();

    Thread thread();

    default int checkpointId() {
        return checkpoint().checkpointId();
    }

    default String actorName() {
        if(thread() instanceof ManagedThread mt) {
            return mt.getActorName();
        }
        throw new IllegalStateException("called outside of a ManagedThread context");
    }
}
