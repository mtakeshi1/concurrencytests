package concurrencytest.checkpoint;

public record ClassAndInstructionsLocation(String className, String methodSignature, int instructionCount) implements CheckpointLocation {
}
