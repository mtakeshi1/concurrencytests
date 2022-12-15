package concurrencytest.runner;

import concurrencytest.checkpoint.instance.CheckpointReached;

@FunctionalInterface
public interface CheckpointReachedCallback {

    void checkpointReached(CheckpointReached checkpointReached) throws Exception;

}
