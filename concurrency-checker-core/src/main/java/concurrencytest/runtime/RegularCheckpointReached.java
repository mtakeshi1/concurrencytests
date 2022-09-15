package concurrencytest.runtime;

import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.CheckpointDescription;

public record RegularCheckpointReached(CheckpointDescription checkpoint, String details, Thread thread) implements CheckpointReached {
}
