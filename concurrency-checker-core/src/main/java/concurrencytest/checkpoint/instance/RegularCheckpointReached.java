package concurrencytest.checkpoint.instance;

import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.instance.CheckpointReached;

public record RegularCheckpointReached(Checkpoint checkpoint, String details, Thread thread) implements CheckpointReached {
}
