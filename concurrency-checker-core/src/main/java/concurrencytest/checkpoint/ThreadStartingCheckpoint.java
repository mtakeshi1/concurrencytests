package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.runtime.thread.ManagedThread;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.checkpoint.instance.ThreadStartCheckpointReached;

public record ThreadStartingCheckpoint(String classUnderEnhancementName, String methodName, String sourceFile,
                                       int lineNumber) implements CheckpointDescription {

    @Override
    public InjectionPoint injectionPoint() {
        return InjectionPoint.AFTER;
    }

    @Override
    public String details() {
        return "";
    }

    @Override
    public CheckpointReached newCheckpointReached(Checkpoint checkpoint, Object context, Thread triggeredThread) {
        return new ThreadStartCheckpointReached(checkpoint, (ManagedThread) context, triggeredThread);
    }
}
