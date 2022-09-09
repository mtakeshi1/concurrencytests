package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public interface MonitorCheckpoint extends Checkpoint {

    Class<?> monitorType();

    InjectionPoint injectionPoint();

    boolean monitorAcquire();

    default boolean monitorRelease() {
        return !monitorAcquire();
    }

}