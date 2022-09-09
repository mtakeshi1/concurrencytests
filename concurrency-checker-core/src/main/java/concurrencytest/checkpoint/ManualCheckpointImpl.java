package concurrencytest.checkpoint;

import concurrencytest.annotations.InjectionPoint;

public class ManualCheckpointImpl extends Checkpoint2 {
    public ManualCheckpointImpl(int checkpointId, String source, int lineNumber) {
        super(checkpointId, InjectionPoint.BEFORE, "", source, lineNumber);
    }
}
