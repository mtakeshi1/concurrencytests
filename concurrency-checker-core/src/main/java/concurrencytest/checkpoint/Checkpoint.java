package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.runtime.checkpoint.CheckpointReached;

import java.io.Serializable;

/**
 * Associates a CheckpointDescription with an id (int)
 */
public record Checkpoint(int checkpointId, CheckpointDescription description) implements Serializable {

    public InjectionPoint injectionPoint() {
        return description().injectionPoint();
    }

    public String details() {
        return description().details();
    }

    public String sourceFile() {
        return description().sourceFile();
    }

    public int lineNumber() {
        return description().lineNumber();
    }

    public CheckpointReached newCheckpointReached(Object context, Thread triggeredThread) {
        return description.newCheckpointReached(this, context, triggeredThread);
    }

}
