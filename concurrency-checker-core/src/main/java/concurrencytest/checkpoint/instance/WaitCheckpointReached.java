package concurrencytest.checkpoint.instance;

import concurrencytest.checkpoint.Checkpoint;

public record WaitCheckpointReached(Checkpoint checkpoint, Object monitorOrLock, Thread thread, boolean monitorWait,
                                    boolean timedWait) implements CheckpointReached {

    @Override
    public String details() {
        return "WAIT for %s ( %s )".formatted(monitorOrLock, monitorOrLock.getClass());
    }


}
