package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record Checkpoint(int checkpointId, CheckpointDescription checkpointDescription) {

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
