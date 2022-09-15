package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record FixedCheckpoint(InjectionPoint injectionPoint, String details) implements CheckpointDescription {

    @Override
    public String sourceFile() {
        return "unknown";
    }

    @Override
    public int lineNumber() {
        return -1;
    }
}
