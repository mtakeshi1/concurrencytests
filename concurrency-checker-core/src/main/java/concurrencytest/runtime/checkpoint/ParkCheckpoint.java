package concurrencytest.runtime.checkpoint;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.CheckpointDescription;

public record ParkCheckpoint(String details, String sourceFile, int lineNumber) implements CheckpointDescription {

    @Override
    public InjectionPoint injectionPoint() {
        return InjectionPoint.AFTER;
    }
}
