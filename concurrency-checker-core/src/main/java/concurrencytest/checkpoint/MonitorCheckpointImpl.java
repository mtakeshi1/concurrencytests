package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record MonitorCheckpointImpl(InjectionPoint injectionPoint, String details, String sourceFile, int lineNumber, String monitorType, boolean monitorAcquire) implements MonitorCheckpoint {
}
