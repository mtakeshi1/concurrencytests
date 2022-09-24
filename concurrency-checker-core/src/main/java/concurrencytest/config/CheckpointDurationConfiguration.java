package concurrencytest.config;

import java.time.Duration;

public record CheckpointDurationConfiguration(Duration checkpointTimeout, Duration maxDurationPerRun, Duration maxTotalDuration) {
    public CheckpointDurationConfiguration() {
        this(Duration.ofMinutes(1), Duration.ofMinutes(10), Duration.ofHours(1));
    }
}
