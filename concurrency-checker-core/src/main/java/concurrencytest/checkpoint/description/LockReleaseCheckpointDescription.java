package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;

public record LockReleaseCheckpointDescription(InjectionPoint injectionPoint, String details, String sourceFile,
                                               int lineNumber) implements CheckpointDescription {

    @Override
    public String toString() {
        return "%s lock release (%s:%d)".formatted(injectionPoint, sourceFile, lineNumber);
    }
}
