package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.CheckpointDescription;

import java.io.Serializable;

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

}
