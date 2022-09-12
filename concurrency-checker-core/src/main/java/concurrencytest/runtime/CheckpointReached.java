package concurrencytest.runtime;

import concurrencytest.checkpoint.Checkpoint;

public interface CheckpointReached {
    Checkpoint checkpoint();

    String details();

    Thread thread();
}
