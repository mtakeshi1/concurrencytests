package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record MonitorCheckpointImpl(int checkpointId, InjectionPoint injectionPoint, String details, String sourceFile, int lineNumber, Class<?> monitorType, boolean monitorAcquire) implements MonitorCheckpoint {
}
