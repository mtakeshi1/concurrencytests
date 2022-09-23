package concurrencytest.runner;

import concurrencytest.runtime.CheckpointReached;
import concurrencytest.runtime.ManagedThread;
import concurrencytest.runtime.RuntimeState;

public interface CheckpointReachedCallback {

    void checkpointReached(ManagedThread managedThread, CheckpointReached checkpointReached, RuntimeState currentState) throws Exception;

}
