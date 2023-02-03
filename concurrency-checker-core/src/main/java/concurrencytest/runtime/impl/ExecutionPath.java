package concurrencytest.runtime.impl;

public record ExecutionPath(String actor, int checkpointId, String details) {
}
