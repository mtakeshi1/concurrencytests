package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.CheckpointDescription;

public record ArrayElementCheckpointDescription(InjectionPoint injectionPoint, String details,
                                                String sourceFile, int lineNumber,
                                                String arrayElementType, boolean arrayLoad) implements CheckpointDescription {

    public boolean arrayStore() {
        return !arrayLoad();
    }

}
