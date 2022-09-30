package concurrencytest.runtime.checkpoint;

import concurrencytest.checkpoint.Checkpoint;

import java.util.concurrent.locks.Lock;

public record LockAcquireReleaseCheckpointReached(Checkpoint checkpoint, Lock theLock, Thread thread) implements CheckpointReached {
    @Override
    public String details() {
        return theLock.toString();
    }
}
