package concurrencytest.runtime;

import concurrencytest.checkpoint.description.CheckpointDescription;

public record LockMonitorAcquisition(int lockOrMonitorId, CheckpointDescription aquisitionCheckpoint, LockType type) {

    enum LockType {
        LOCK, MONITOR
    }

    @Override
    public String toString() {
        return "%S id=%d acquisition at: %s:%d ".formatted(type, lockOrMonitorId, aquisitionCheckpoint.sourceFile(), aquisitionCheckpoint.lineNumber());
    }
}
