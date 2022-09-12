package concurrencytest.runtime;

import concurrencytest.checkpoint.Checkpoint;

public record RegularCheckpointReached(Checkpoint checkpoint, String details, Thread thread) implements CheckpointReached {
}
