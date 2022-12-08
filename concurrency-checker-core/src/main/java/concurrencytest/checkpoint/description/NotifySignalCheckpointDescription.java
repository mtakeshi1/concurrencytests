package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;

public record NotifySignalCheckpointDescription(String details, String sourceFile, int lineNumber, boolean monitorNotify) implements CheckpointDescription {

    @Override
    public InjectionPoint injectionPoint() {
        return InjectionPoint.BEFORE;
    }
}
