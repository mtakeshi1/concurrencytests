package concurrencytest.runtime.checkpoint;

import concurrencytest.checkpoint.Checkpoint;

public record MonitorCheckpointReached(Checkpoint checkpoint, Object monitorOwner,
                                       Thread thread) implements CheckpointReached {

    @Override
    public String details() {
        return "MONITOR for %s ( %s )".formatted(monitorOwner, monitorOwner.getClass());
    }
}
