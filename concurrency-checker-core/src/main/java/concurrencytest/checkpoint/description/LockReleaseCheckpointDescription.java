package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.instance.CheckpointReached;

import java.util.concurrent.locks.Lock;

public record LockReleaseCheckpointDescription(InjectionPoint injectionPoint, String details, String sourceFile,
                                               int lineNumber) implements CheckpointDescription {

    @Override
    public String toString() {
        return "%s lock release (%s:%d)".formatted(injectionPoint, sourceFile, lineNumber);
    }

    @Override
    public CheckpointReached newCheckpointReached(Checkpoint checkpoint, Object context, Thread triggeredThread) {
        //return new RegularCheckpointReached(checkpoint, String.valueOf(context), triggeredThread);
        return new LockReleasedCheckpointReached(checkpoint, (Lock) context, triggeredThread);
    }
}
