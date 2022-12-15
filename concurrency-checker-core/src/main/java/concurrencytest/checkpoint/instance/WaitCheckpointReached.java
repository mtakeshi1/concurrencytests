package concurrencytest.checkpoint.instance;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.Checkpoint;

public record WaitCheckpointReached(Checkpoint checkpoint, Object monitorOrLock, Thread thread, boolean monitorWait, boolean timedWait,
                                    InjectionPoint injectionPoint) implements CheckpointReached {

    @Override
    public String details() {
        return "WAIT for %s ( %s )".formatted(monitorOrLock, monitorOrLock.getClass());
    }


}
