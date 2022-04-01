package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

import java.util.concurrent.locks.Lock;

public interface LockCheckpoint extends Checkpoint {

    Class<? extends Lock> lockType();

    InjectionPoint injectionPoint();

    boolean lockAcquire();

    default boolean lockRelease() {
        return !lockAcquire();
    }
}
