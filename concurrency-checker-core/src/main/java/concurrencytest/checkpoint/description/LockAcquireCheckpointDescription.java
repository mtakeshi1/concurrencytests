package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.asm.TryAcquireWithResult;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.runtime.checkpoint.CheckpointReached;
import concurrencytest.runtime.checkpoint.LockAcquireReleaseCheckpointReached;

import java.util.concurrent.locks.Lock;

public record LockAcquireCheckpointDescription(InjectionPoint injectionPoint, String details, String sourceFile, int lineNumber, boolean acquireTry,
                                               boolean timedAcquire) implements CheckpointDescription {
    @Override
    public CheckpointReached newCheckpointReached(Checkpoint checkpoint, Object context, Thread triggeredThread) {
        return new LockAcquireReleaseCheckpointReached(checkpoint, (TryAcquireWithResult) context, triggeredThread);
    }

    @Override
    public String toString() {
        return "%s %s lock acquisition (%s:%d)".formatted(injectionPoint, timedAcquire ? "timed" : "", sourceFile, lineNumber);
    }
}
