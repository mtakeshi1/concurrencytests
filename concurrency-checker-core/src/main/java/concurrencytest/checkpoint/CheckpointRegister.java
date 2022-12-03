package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.CheckpointDescription;
import org.objectweb.asm.Type;

import java.lang.reflect.Member;
import java.util.Map;
import java.util.TreeMap;

public interface CheckpointRegister {

    Checkpoint newFieldCheckpoint(InjectionPoint injectionPoint,Type declaringClass, String fieldName, Type fieldType, boolean read, String details, String sourceFile, int lineNumber);

    Map<CheckpointDescription, Checkpoint> allCheckpoints();

    default Map<Integer, Checkpoint> checkpointsById() {
        Map<Integer, Checkpoint> map = new TreeMap<>();
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

    Checkpoint newMonitorEnterCheckpoint(Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint);

    Checkpoint newMonitorExitCheckpoint(Class<?> classUnderEnhancement, String methodName, String methodDescriptor, Type monitorOwnerType, String sourceName, int latestLineNumber, InjectionPoint injectionPoint);

    Checkpoint newMethodCheckpoint(String sourceName, int latestLineNumber, Member method, InjectionPoint before);

    Checkpoint newParkCheckpoint(String details, String sourceName, int latestLineNumber);

    Checkpoint newObjectWaitCheckpoint(String sourceName, int latestLineNumber);

    Checkpoint arrayElementCheckpoint(InjectionPoint injectionPoint, boolean arrayRead, Class<?> arrayType, String sourceName, int latestLineNumber);

    Checkpoint managedThreadStartedCheckpoint(String classUnderEnhancementName, String methodName, String methodDescriptor, String sourceName, int latestLineNumber);

    default boolean isFinishedCheckpoint(int checkpointId) {
        return checkpointId == taskFinishedCheckpoint().checkpointId();
    }

    Checkpoint newLockAcquireCheckpoint(InjectionPoint before, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, String sourceName, int latestLineNumber);

    Checkpoint newLockReleasedCheckpoint(InjectionPoint injectionPoint, Class<?> classUnderEnhancement, String methodName, String methodDescriptor, String sourceName, int latestLineNumber);

    Checkpoint newNotifyCheckpoint(boolean notifyAll, String sourceName, int latestLineNumber);
}
