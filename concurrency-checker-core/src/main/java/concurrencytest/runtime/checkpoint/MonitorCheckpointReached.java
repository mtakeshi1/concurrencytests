package concurrencytest.runtime.checkpoint;

import concurrencytest.checkpoint.description.MonitorCheckpointDescription;

public record MonitorCheckpointReached(MonitorCheckpointDescription checkpoint, Object monitorOwner,
                                       Thread thread) implements CheckpointReached {

    @Override
    public String details() {
        return "MONITOR for %s ( %s )".formatted(monitorOwner, monitorOwner.getClass());
    }
}
