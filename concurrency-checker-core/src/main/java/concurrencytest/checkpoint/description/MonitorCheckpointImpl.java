package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.MonitorCheckpointDescription;

public record MonitorCheckpointImpl(InjectionPoint injectionPoint, String details, String sourceFile, int lineNumber, String monitorType, boolean monitorAcquire) implements MonitorCheckpointDescription {
}
