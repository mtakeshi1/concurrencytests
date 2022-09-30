package concurrencytest.runtime;

import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.ThreadStartingCheckpoint;
import concurrencytest.checkpoint.description.MonitorCheckpointDescription;
import concurrencytest.runner.CheckpointReachedCallback;
import concurrencytest.runtime.checkpoint.CheckpointReached;
import concurrencytest.runtime.checkpoint.MonitorCheckpointReached;
import concurrencytest.runtime.checkpoint.RegularCheckpointReached;
import concurrencytest.runtime.checkpoint.ThreadStartCheckpointReached;
import org.junit.Assert;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RecordingCheckpointRuntime implements CheckpointRuntime {

    private final CheckpointRegister checkpointRegister;

    private final List<CheckpointReached> checkpoints = new CopyOnWriteArrayList<>();

    private final List<CheckpointReachedCallback> callbacks = new CopyOnWriteArrayList<>();

    public List<CheckpointReached> getCheckpoints() {
        return checkpoints;
    }

    public RecordingCheckpointRuntime(CheckpointRegister checkpointRegister) {
        this.checkpointRegister = checkpointRegister;
    }

    @Override
    public void beforeActorStartCheckpoint() {
        Checkpoint checkpoint = checkpointRegister.taskStartingCheckpoint();
        checkpoints.add(new RegularCheckpointReached(checkpoint.checkpointDescription(), "", Thread.currentThread()));
    }

    @Override
    public void actorFinishedCheckpoint() {
        Checkpoint checkpoint = checkpointRegister.taskFinishedCheckpoint();
        checkpoints.add(new RegularCheckpointReached(checkpoint.checkpointDescription(), "", Thread.currentThread()));
    }

    @Override
    public void checkpointReached(int id) {
        Checkpoint checkpoint = checkpointRegister.checkpointById(id);
        Assert.assertNotNull("checkpoint not found: " + id, checkpoint);
        checkpoints.add(new RegularCheckpointReached(checkpoint.checkpointDescription(), "", Thread.currentThread()));
    }

    @Override
    public void checkpointReached(int id, Object details) {
        Checkpoint checkpoint = checkpointRegister.checkpointById(id);
        Assert.assertNotNull("checkpoint not found: " + id, checkpoint);
        if (checkpoint.checkpointDescription() instanceof MonitorCheckpointDescription monitorCheckpoint) {
            checkpoints.add(new MonitorCheckpointReached(monitorCheckpoint, details, Thread.currentThread()));
        } else if (details instanceof ManagedThread mn && checkpoint.checkpointDescription() instanceof ThreadStartingCheckpoint) {
            checkpoints.add(new ThreadStartCheckpointReached(checkpoint.checkpointDescription(), mn, Thread.currentThread()));
        } else {
            checkpoints.add(new RegularCheckpointReached(checkpoint.checkpointDescription(), String.valueOf(details), Thread.currentThread()));
        }
    }

    @Override
    public void fieldAccessCheckpoint(int checkpointId, Object owner, Object value) {
        throw new RuntimeException("not yet implemented");
    }

    @Override
    public void addCheckpointCallback(CheckpointReachedCallback basicRuntimeState) {
        callbacks.add(basicRuntimeState);
    }

    @Override
    public void removeCheckpointCallback(CheckpointReachedCallback basicRuntimeState) {
        callbacks.remove(basicRuntimeState);
    }
}
