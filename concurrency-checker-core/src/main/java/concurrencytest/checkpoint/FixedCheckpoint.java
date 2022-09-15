package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record FixedCheckpoint(int checkpointId, InjectionPoint injectionPoint, String details) implements Checkpoint {

    @Override
    public String sourceFile() {
        return "unknown";
    }

    @Override
    public int lineNumber() {
        return -1;
    }
}
