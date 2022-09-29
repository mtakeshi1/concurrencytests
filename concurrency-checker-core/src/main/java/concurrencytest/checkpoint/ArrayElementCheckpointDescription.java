package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public record ArrayElementCheckpointDescription(InjectionPoint injectionPoint, String details,
                                                String sourceFile, int lineNumber,
                                                String arrayElementType, boolean arrayLoad) implements CheckpointDescription {

    public boolean arrayStore() {
        return !arrayLoad();
    }

}
