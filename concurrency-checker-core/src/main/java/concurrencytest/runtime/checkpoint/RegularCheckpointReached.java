package concurrencytest.runtime.checkpoint;

import concurrencytest.checkpoint.description.CheckpointDescription;

public record RegularCheckpointReached(CheckpointDescription checkpoint, String details, Thread thread) implements CheckpointReached {
}
