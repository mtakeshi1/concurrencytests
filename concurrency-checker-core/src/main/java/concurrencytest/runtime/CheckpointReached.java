package concurrencytest.runtime;

import concurrencytest.checkpoint.CheckpointDescription;

public interface CheckpointReached {
    CheckpointDescription checkpoint();

    String details();

    Thread thread();
}
