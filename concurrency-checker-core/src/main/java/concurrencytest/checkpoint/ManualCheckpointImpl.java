package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record ManualCheckpointImpl(int checkpointId, String details, String sourceFile,
                                   int lineNumber) implements Checkpoint {
    public InjectionPoint injectionPoint() {
        return InjectionPoint.AFTER;
    }
}



