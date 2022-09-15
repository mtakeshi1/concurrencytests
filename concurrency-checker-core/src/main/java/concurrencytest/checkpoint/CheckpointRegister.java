package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.Map;

public interface CheckpointRegister {

    FieldAccessCheckpoint newFieldCheckpoint(InjectionPoint injectionPoint, Class<?> declaringClass, String fieldName, Class<?> fieldType, boolean read, String details, String sourceFile, int lineNumber);

    Map<Integer, Checkpoint> allCheckpoints();

    default Checkpoint checkpointById(int id) {
        return allCheckpoints().get(id);
    }

    Checkpoint taskStartingCheckpoint();
    Checkpoint taskFinishedCheckpoint();

    //TODO not sure if details is needed for manual checkpoint
    Checkpoint newManualCheckpoint(String details, String source, int latestLineNumber);

    MonitorCheckpoint newMonitorEnterCheckpoint(InjectionPoint point, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint);

    MonitorCheckpoint newMonitorExitCheckpoint(InjectionPoint point, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint);

    Checkpoint newMethodCheckpoint(String sourceName, int latestLineNumber, Method method, InjectionPoint before);

    Checkpoint newParkCheckpoint(String details, String sourceName, int latestLineNumber);
}
