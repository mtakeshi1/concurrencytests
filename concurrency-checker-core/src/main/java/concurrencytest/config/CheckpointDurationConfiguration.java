package concurrencytest.config;

import java.time.Duration;

/**
 * Controls the various checkpoints
 *
 * @param checkpointTimeout how long to wait for actors to reach checkpoints
 * @param maxDurationPerRun maximum duration of a single execution
 * @param maxTotalDuration maximum duration of the whole test
 */
public record CheckpointDurationConfiguration(Duration checkpointTimeout, Duration maxDurationPerRun, Duration maxTotalDuration) {
    public CheckpointDurationConfiguration() {
        this(Duration.ofMinutes(1), Duration.ofMinutes(10), Duration.ofHours(1));
    }
}
