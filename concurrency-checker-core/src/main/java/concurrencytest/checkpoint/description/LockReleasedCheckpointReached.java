package concurrencytest.checkpoint.description;

import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.runtime.checkpoint.CheckpointReached;

import java.util.concurrent.locks.Lock;

public record LockReleasedCheckpointReached(Checkpoint checkpoint, Lock theLock, Thread thread) implements LockCheckpointReached {

    @Override
    public String details() {
        return String.valueOf(theLock);
    }
}
