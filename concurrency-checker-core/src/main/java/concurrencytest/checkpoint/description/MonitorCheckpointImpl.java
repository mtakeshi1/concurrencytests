package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.runtime.checkpoint.CheckpointReached;
import concurrencytest.runtime.checkpoint.MonitorCheckpointReached;

public record MonitorCheckpointImpl(InjectionPoint injectionPoint, String details, String sourceFile, int lineNumber,
                                    boolean monitorAcquire) implements MonitorCheckpointDescription {
    @Override
    public CheckpointReached newCheckpointReached(Checkpoint checkpoint, Object context, Thread triggeredThread) {
        return new MonitorCheckpointReached(checkpoint, context, triggeredThread);
    }

    @Override
    public String toString() {
        return "%s monitor %s (%s:%d)".formatted(
                injectionPoint.name(), monitorAcquire ? "ACQUIRE" : "RELEASE", sourceFile, lineNumber
        );
    }
}
