package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.FieldAccessCheckpoint;

public record FieldAccessCheckpointImpl(InjectionPoint injectionPoint, String details,
                                        String sourceFile, int lineNumber,
                                        String declaringClass, String fieldName, String fieldType,
                                        boolean fieldRead) implements FieldAccessCheckpoint {
    @Override
    public String toString() {
        String staticField = details.contains("static") ? "static" : "";
        String readWrite = fieldRead ? "read" : "write";
        return "%s %s %s %s.%s (%s:%d)".formatted(injectionPoint, staticField, readWrite, declaringClass, fieldName, sourceFile, lineNumber);
    }
}
