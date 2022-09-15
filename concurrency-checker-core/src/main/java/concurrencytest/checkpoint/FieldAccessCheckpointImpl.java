package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record FieldAccessCheckpointImpl(InjectionPoint injectionPoint, String details,
                                        String sourceFile, int lineNumber,
                                        Class<?> declaringClass, String fieldName, Class<?> fieldType,
                                        boolean fieldRead) implements FieldAccessCheckpoint {

}
