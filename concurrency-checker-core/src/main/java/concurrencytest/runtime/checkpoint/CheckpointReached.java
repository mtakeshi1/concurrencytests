package concurrencytest.runtime.checkpoint;

import concurrencytest.checkpoint.description.CheckpointDescription;

public interface CheckpointReached {
    CheckpointDescription checkpoint();

    String details();

    Thread thread();
}
