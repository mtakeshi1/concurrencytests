package concurrencytest.runner;

import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.runtime.CheckpointRuntime;
import concurrencytest.runtime.StandardCheckpointRuntime;
import concurrencytest.runtime.checkpoint.CheckpointReached;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingCheckpointRuntime extends StandardCheckpointRuntime implements CheckpointRuntime {


    private final List<CheckpointReached> checkpoints = new CopyOnWriteArrayList<>();

    public List<CheckpointReached> getCheckpoints() {
        return checkpoints;
    }

    public RecordingCheckpointRuntime(CheckpointRegister checkpointRegister) {
        super(checkpointRegister);
        addCheckpointCallback(checkpoints::add);
    }

}
