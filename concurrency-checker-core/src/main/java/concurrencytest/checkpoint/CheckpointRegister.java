package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

import java.util.Map;

public interface CheckpointRegister {

    FieldAccessCheckpoint newFieldCheckpoint(InjectionPoint injectionPoint, Class<?> declaringClass, String fieldName, Class<?> fieldType, boolean read, String details, String sourceFile, int lineNumber);

    Map<Integer, Checkpoint> allCheckpoints();

    default Checkpoint checkpointById(int id) {
        return allCheckpoints().get(id);
    }

    Checkpoint newManualCheckpoint(String source, int latestLineNumber);
}
