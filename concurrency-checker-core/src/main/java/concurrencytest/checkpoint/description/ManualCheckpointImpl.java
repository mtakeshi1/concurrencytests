package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.CheckpointDescription;

public record ManualCheckpointImpl(String details, String sourceFile, int lineNumber) implements CheckpointDescription {
    public InjectionPoint injectionPoint() {
        return InjectionPoint.AFTER;
    }

    @Override
    public String toString() {
        return "ManualCheckpoint (%s:%d)".formatted(sourceFile, lineNumber);
    }
}



