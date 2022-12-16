package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.checkpoint.instance.NotifySignalCheckpoint;

public record NotifySignalCheckpointDescription(String sourceFile, int lineNumber, boolean monitorNotify, boolean notifySignalAll) implements CheckpointDescription {

    @Override
    public InjectionPoint injectionPoint() {
        return InjectionPoint.BEFORE;
    }

    @Override
    public String details() {
        return "";
    }


    @Override
    public CheckpointReached newCheckpointReached(Checkpoint checkpoint, Object context, Thread triggeredThread) {
        return new NotifySignalCheckpoint(checkpoint, context, triggeredThread, monitorNotify, notifySignalAll);
    }
}
