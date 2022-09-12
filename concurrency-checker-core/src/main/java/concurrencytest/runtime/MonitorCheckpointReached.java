package concurrencytest.runtime;

import concurrencytest.checkpoint.MonitorCheckpoint;

public record MonitorCheckpointReached(MonitorCheckpoint checkpoint, Object monitorOwner,
                                       Thread thread) implements CheckpointReached {

    @Override
    public String details() {
        return "MONITOR for %s ( %s )".formatted(monitorOwner, monitorOwner.getClass());
    }
}
