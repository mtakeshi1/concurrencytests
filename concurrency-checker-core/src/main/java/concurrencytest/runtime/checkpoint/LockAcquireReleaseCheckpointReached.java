package concurrencytest.runtime.checkpoint;

import concurrencytest.asm.TryAcquireWithResult;
import concurrencytest.checkpoint.Checkpoint;

import java.util.concurrent.locks.Lock;

public record LockAcquireReleaseCheckpointReached(Checkpoint checkpoint, TryAcquireWithResult result, Thread thread) implements CheckpointReached {
    @Override
    public String details() {
        return theLock().toString();
    }

    public Lock theLock() {
        return result.lock();
    }

}
