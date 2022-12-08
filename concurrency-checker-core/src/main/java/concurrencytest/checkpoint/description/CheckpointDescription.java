package concurrencytest.checkpoint.description;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.checkpoint.instance.RegularCheckpointReached;

import java.io.Serializable;

public interface CheckpointDescription extends Serializable {

    InjectionPoint injectionPoint();

    String details();

    String sourceFile();

    int lineNumber();

    default CheckpointReached newCheckpointReached(Checkpoint checkpoint, Object context, Thread triggeredThread) {
        return new RegularCheckpointReached(checkpoint, String.valueOf(context), triggeredThread);
    }

}
