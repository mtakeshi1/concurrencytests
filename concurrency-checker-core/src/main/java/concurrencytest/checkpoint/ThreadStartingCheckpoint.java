package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.CheckpointDescription;

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
