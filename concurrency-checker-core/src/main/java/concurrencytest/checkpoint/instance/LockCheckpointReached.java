package concurrencytest.checkpoint.instance;

import java.util.concurrent.locks.Lock;

public interface LockCheckpointReached extends CheckpointReached {

    Lock theLock();

}
