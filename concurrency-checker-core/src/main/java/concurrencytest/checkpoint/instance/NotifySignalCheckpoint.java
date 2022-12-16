package concurrencytest.checkpoint.instance;

import concurrencytest.checkpoint.Checkpoint;

public record NotifySignalCheckpoint(Checkpoint checkpoint, Object monitorOrLock, Thread thread, boolean monitorWait,
                                     boolean signalAll) implements CheckpointReached {

    @Override
    public String details() {
        return "NOTIFY%s on %s (%s)".formatted(signalAll ? "_ALL" : "", monitorOrLock, monitorOrLock.getClass());
    }
}
