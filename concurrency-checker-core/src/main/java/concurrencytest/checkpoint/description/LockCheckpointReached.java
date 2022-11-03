package concurrencytest.checkpoint.description;

import concurrencytest.runtime.checkpoint.CheckpointReached;

import java.util.concurrent.locks.Lock;

public interface LockCheckpointReached extends CheckpointReached {

    Lock theLock();

}
