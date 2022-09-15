package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record MonitorCheckpointImpl(InjectionPoint injectionPoint, String details, String sourceFile, int lineNumber, Class<?> monitorType, boolean monitorAcquire) implements MonitorCheckpoint {
}
