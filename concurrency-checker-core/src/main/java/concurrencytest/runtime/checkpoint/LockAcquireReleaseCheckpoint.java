package concurrencytest.runtime.checkpoint;

import concurrencytest.checkpoint.description.CheckpointDescription;

import java.util.concurrent.locks.Lock;

public record LockAcquireReleaseCheckpoint(CheckpointDescription checkpoint, Lock theLock, Thread thread) implements CheckpointReached {
    @Override
    public String details() {
        return theLock.toString();
    }
}
