package concurrencytest.checkpoint.instance;

import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.runtime.thread.ManagedThread;

/**
 * Adds context information to a Checkpoint
 */
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
