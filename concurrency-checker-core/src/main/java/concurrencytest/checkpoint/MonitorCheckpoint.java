package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public interface MonitorCheckpoint extends CheckpointDescription {

    String monitorType();

    InjectionPoint injectionPoint();

    boolean monitorAcquire();

    default boolean isMonitorRelease() {
        return !monitorAcquire();
    }

}
