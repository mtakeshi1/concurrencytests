package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;

public record NotifySignalCheckpointDescription(String sourceFile, int lineNumber, boolean monitorNotify, boolean notifySignalAll) implements CheckpointDescription {

    @Override
    public InjectionPoint injectionPoint() {
        return InjectionPoint.BEFORE;
    }

    @Override
    public String details() {
        return "";
    }
}
