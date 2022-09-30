package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;

public record LockReleaseCheckpointDescription(InjectionPoint injectionPoint, String details, String sourceFile,
                                               int lineNumber) implements CheckpointDescription {
}
