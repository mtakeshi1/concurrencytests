package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.CheckpointDescription;

public record FixedCheckpoint(InjectionPoint injectionPoint, String details) implements CheckpointDescription {

    @Override
    public String sourceFile() {
        return "unknown";
    }

    @Override
    public int lineNumber() {
        return -1;
    }

    @Override
    public String toString() {
        return details;
    }
}
