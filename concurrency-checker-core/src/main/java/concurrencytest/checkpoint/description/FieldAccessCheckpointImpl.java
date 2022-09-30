package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.FieldAccessCheckpoint;

public record FieldAccessCheckpointImpl(InjectionPoint injectionPoint, String details,
                                        String sourceFile, int lineNumber,
                                        String declaringClass, String fieldName, String fieldType,
                                        boolean fieldRead) implements FieldAccessCheckpoint {

}
