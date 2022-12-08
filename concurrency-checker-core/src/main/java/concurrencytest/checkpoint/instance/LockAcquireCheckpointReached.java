package concurrencytest.checkpoint.instance;

import concurrencytest.checkpoint.instance.LockCheckpointReached;
import concurrencytest.checkpoint.description.TryAcquireWithResult;
import concurrencytest.checkpoint.Checkpoint;

import java.util.concurrent.locks.Lock;

public record LockAcquireCheckpointReached(Checkpoint checkpoint, TryAcquireWithResult result, Thread thread) implements LockCheckpointReached {
    @Override
    public String details() {
        return theLock().toString();
    }

    public Lock theLock() {
        return result.lock();
    }

}
