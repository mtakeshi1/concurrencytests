package concurrencytest.runner;

import concurrencytest.runtime.checkpoint.CheckpointReached;

public interface CheckpointReachedCallback {

    void checkpointReached(CheckpointReached checkpointReached) throws Exception;

}
