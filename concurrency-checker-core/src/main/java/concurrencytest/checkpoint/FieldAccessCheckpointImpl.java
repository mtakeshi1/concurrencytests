package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record FieldAccessCheckpointImpl(InjectionPoint injectionPoint, String details,
                                        String sourceFile, int lineNumber,
                                        String declaringClass, String fieldName, String fieldType,
                                        boolean fieldRead) implements FieldAccessCheckpoint {

}
