package concurrencytest.checkpoint.instance;

import concurrencytest.checkpoint.Checkpoint;

public record MonitorCheckpointReached(Checkpoint checkpoint, Object monitorOwner,
                                       Thread thread) implements CheckpointReached {

    @Override
    public String details() {
        return "class=%s, identity=%d".formatted(monitorOwner.getClass().getName(), System.identityHashCode(monitorOwner));
    }
}
