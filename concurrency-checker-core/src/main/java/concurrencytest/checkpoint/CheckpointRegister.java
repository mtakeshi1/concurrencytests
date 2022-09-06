package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

import java.util.Map;

public interface CheckpointRegister {

    FieldAccessCheckpoint newFieldCheckpoint(InjectionPoint injectionPoint, Class<?> declaringClass, String fieldName, Class<?> fieldType, boolean read, String details, String sourceFile, int lineNumber);

    Map<Long, Checkpoint> allCheckpoints();

    default Checkpoint checkpointById(long id) {
        return allCheckpoints().get(id);
    }

}
