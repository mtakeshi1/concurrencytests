package concurrencytest.checkpoint;

public record SourceCodeLineLocation(String className, String methodSignature, String sourceFileName, int lineNumber) implements CheckpointLocation {
}
