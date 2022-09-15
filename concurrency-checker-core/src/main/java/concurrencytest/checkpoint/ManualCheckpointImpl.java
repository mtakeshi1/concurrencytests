package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record ManualCheckpointImpl(String details, String sourceFile,
                                   int lineNumber) implements CheckpointDescription {
    public InjectionPoint injectionPoint() {
        return InjectionPoint.AFTER;
    }
}



