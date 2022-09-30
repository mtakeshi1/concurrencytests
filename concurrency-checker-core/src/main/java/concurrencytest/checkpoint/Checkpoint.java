package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.CheckpointDescription;

import java.io.Serializable;

public record Checkpoint(int checkpointId, CheckpointDescription checkpointDescription) implements Serializable {

    public InjectionPoint injectionPoint() {
        return checkpointDescription().injectionPoint();
    }

    public String details() {
        return checkpointDescription().details();
    }

    public String sourceFile() {
        return checkpointDescription().sourceFile();
    }

    public int lineNumber() {
        return checkpointDescription().lineNumber();
    }

}
