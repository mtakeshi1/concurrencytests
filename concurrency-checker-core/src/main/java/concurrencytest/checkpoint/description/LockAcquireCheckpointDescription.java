package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;

public record LockAcquireCheckpointDescription(InjectionPoint injectionPoint, String details, String sourceFile, int lineNumber, boolean acquireTry,
                                               boolean timedAcquire) implements CheckpointDescription {
}
