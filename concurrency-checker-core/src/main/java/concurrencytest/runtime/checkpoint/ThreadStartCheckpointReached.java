package concurrencytest.runtime.checkpoint;

import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.runtime.thread.ManagedThread;

/**
 * Signals that a new Thread has started.
 *
 * @param checkpoint the checkpoint that triggered this new thread
 * @param freshThread the new ManagedThread that started
 * @param thread the thread that started another thread
 */
public record ThreadStartCheckpointReached(Checkpoint checkpoint, ManagedThread freshThread, Thread thread) implements CheckpointReached {
    @Override
    public String details() {
        return freshThread.getActorName();
    }

    public String newActorName() {
        return freshThread.getActorName();
    }
}
