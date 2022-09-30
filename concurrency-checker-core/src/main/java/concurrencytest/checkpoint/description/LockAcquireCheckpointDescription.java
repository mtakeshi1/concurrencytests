package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.runtime.checkpoint.CheckpointReached;
import concurrencytest.runtime.checkpoint.LockAcquireReleaseCheckpointReached;

import java.util.concurrent.locks.Lock;

public record LockAcquireCheckpointDescription(InjectionPoint injectionPoint, String details, String sourceFile, int lineNumber, boolean acquireTry,
                                               boolean timedAcquire) implements CheckpointDescription {
    @Override
    public CheckpointReached newCheckpointReached(Checkpoint checkpoint, Object context, Thread triggeredThread) {
        return new LockAcquireReleaseCheckpointReached(checkpoint, (Lock) context, triggeredThread);
    }
}