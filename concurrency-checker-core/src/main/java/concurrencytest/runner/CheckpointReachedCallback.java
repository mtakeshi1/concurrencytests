package concurrencytest.runner;

import concurrencytest.checkpoint.instance.CheckpointReached;

public interface CheckpointReachedCallback {

    void checkpointReached(CheckpointReached checkpointReached) throws Exception;

}
