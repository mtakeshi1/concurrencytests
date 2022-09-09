package concurrencytest.runtime;

import concurrencytest.checkpoint.Checkpoint;

public record CheckpointReached(Checkpoint checkpoint, String details, Thread thread) {
}
