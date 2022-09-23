package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record ThreadStartingCheckpoint(String classUnderEnhancementName, String methodName, String sourceFile,
                                       int lineNumber) implements CheckpointDescription {

    @Override
    public InjectionPoint injectionPoint() {
        return InjectionPoint.AFTER;
    }

    @Override
    public String details() {
        return "";
    }

}
