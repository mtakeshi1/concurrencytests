package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;

public interface MonitorCheckpointDescription extends CheckpointDescription {

    String monitorType();

    InjectionPoint injectionPoint();

    boolean monitorAcquire();

    default boolean isMonitorRelease() {
        return !monitorAcquire();
    }

}
