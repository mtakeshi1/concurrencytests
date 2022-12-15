package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.checkpoint.instance.WaitCheckpointReached;

public record WaitAwaitCheckpointDescription(String sourceFile, int lineNumber, boolean monitorWait, boolean timedWait, InjectionPoint injectionPoint) implements CheckpointDescription {

    @Override
    public String details() {
        return "";
    }

    @Override
    public CheckpointReached newCheckpointReached(Checkpoint checkpoint, Object context, Thread triggeredThread) {
        return new WaitCheckpointReached(checkpoint, context, triggeredThread, this.monitorWait, timedWait, this.injectionPoint);
    }
}
