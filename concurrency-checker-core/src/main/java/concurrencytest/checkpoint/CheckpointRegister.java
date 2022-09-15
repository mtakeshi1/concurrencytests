package concurrencytest.checkpoint;

import com.sun.source.tree.BreakTree;
import concurrencytest.annotations.InjectionPoint;
import org.objectweb.asm.Type;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public interface CheckpointRegister {

    Checkpoint newFieldCheckpoint(InjectionPoint injectionPoint, Class<?> declaringClass, String fieldName, Class<?> fieldType, boolean read, String details, String sourceFile, int lineNumber);

    Map<CheckpointDescription, Checkpoint> allCheckpoints();

    default Map<Integer, Checkpoint> checkpointsById() {
        Map<Integer, Checkpoint> map = new HashMap<>();
        allCheckpoints().forEach((ignored, v) -> map.put(v.checkpointId(), v));
        return map;
    }

    default Checkpoint checkpointById(int id) {
        return checkpointsById().get(id);
    }

    Checkpoint taskStartingCheckpoint();

    Checkpoint taskFinishedCheckpoint();

    //TODO not sure if details is needed for manual checkpoint
    Checkpoint newManualCheckpoint(String details, String source, int latestLineNumber);

    Checkpoint newMonitorEnterCheckpoint(InjectionPoint point, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint);

    Checkpoint newMonitorExitCheckpoint(InjectionPoint point, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint);

    Checkpoint newMethodCheckpoint(String sourceName, int latestLineNumber, Method method, InjectionPoint before);

    Checkpoint newParkCheckpoint(String details, String sourceName, int latestLineNumber);

    Checkpoint arrayElementCheckpoint(InjectionPoint injectionPoint, boolean arrayRead, Class<?> arrayType, String sourceName, int latestLineNumber);
}
