package concurrencytest.runtime;

import concurrencytest.checkpoint.description.CheckpointDescription;

public record LockMonitorAcquisition(int lockOrMonitorId, CheckpointDescription aquisitionCheckpoint) {
}
