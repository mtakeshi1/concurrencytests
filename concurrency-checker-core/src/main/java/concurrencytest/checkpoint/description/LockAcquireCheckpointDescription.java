package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.checkpoint.instance.LockAcquireCheckpointReached;

public record LockAcquireCheckpointDescription(InjectionPoint injectionPoint, String details, String sourceFile, int lineNumber, boolean acquireTry,
                                               boolean timedAcquire) implements CheckpointDescription {
    @Override
    public CheckpointReached newCheckpointReached(Checkpoint checkpoint, Object context, Thread triggeredThread) {
        return new LockAcquireCheckpointReached(checkpoint, (TryAcquireWithResult) context, triggeredThread);
    }

    @Override
    public String toString() {
        return "%s %s lock acquisition (%s:%d)".formatted(injectionPoint, timedAcquire ? "timed" : "", sourceFile, lineNumber);
    }
}
