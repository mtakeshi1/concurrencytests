package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;

public record WaitAwaitCheckpointDescription(String details, String sourceFile, int lineNumber, boolean monitorWait) implements CheckpointDescription {
    @Override
    public InjectionPoint injectionPoint() {
        return InjectionPoint.BEFORE;
    }
}
