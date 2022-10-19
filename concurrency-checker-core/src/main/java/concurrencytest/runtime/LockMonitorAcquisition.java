package concurrencytest.runtime;

import concurrencytest.checkpoint.description.CheckpointDescription;

public record LockMonitorAcquisition(int lockOrMonitorId, CheckpointDescription aquisitionCheckpoint) {

    @Override
    public String toString() {
        return "LockMonitorAcquisition{ %d acquired at: %s:%d ".formatted(lockOrMonitorId, aquisitionCheckpoint.sourceFile(), aquisitionCheckpoint.lineNumber());
    }
}
