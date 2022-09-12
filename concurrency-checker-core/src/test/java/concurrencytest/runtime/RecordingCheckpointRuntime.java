package concurrencytest.runtime;

import concurrencytest.checkpoint.Checkpoint;
import concurrencytest.checkpoint.CheckpointRegister;
import concurrencytest.checkpoint.MonitorCheckpoint;
import concurrencytest.runtime.CheckpointReached;
import concurrencytest.runtime.CheckpointRuntime;
import concurrencytest.runtime.MonitorCheckpointReached;
import concurrencytest.runtime.RegularCheckpointReached;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class RecordingCheckpointRuntime implements CheckpointRuntime {

    private final CheckpointRegister checkpointRegister;

    private final List<CheckpointReached> checkpoints = new ArrayList<>();

    public List<CheckpointReached> getCheckpoints() {
        return checkpoints;
    }

    public RecordingCheckpointRuntime(CheckpointRegister checkpointRegister) {
        this.checkpointRegister = checkpointRegister;
    }

//        @Override
//        public void beforeMonitorAcquiredCheckpoint(Object monitor, int id) {
//            Checkpoint checkpoint = checkpointRegister.checkpointById(id);
//            if (checkpoint == null) {
//                throw new IllegalArgumentException("Unknown checkpoint with id %d".formatted(id));
//            } else if (checkpoint instanceof MonitorCheckpoint monitorCheckpoint) {
//                MonitorCheckpointReached reached = new MonitorCheckpointReached(monitorCheckpoint, monitor, Thread.currentThread());
//                checkpoints.add(reached);
//            } else {
//                throw new IllegalArgumentException("Checkpoint with id %d should've been a monitor checkpoint but was: %s".formatted(id, checkpoint.getClass()));
//            }
//        }
//
//        @Override
//        public void afterMonitorReleasedCheckpoint(Object monitor, int id) {
//            Checkpoint checkpoint = checkpointRegister.checkpointById(id);
//            if (checkpoint == null) {
//                throw new IllegalArgumentException("Unknown checkpoint with id %d".formatted(id));
//            } else if (checkpoint instanceof MonitorCheckpoint monitorCheckpoint) {
//                MonitorCheckpointReached reached = new MonitorCheckpointReached(monitorCheckpoint, monitor, Thread.currentThread());
//                checkpoints.add(reached);
//            } else {
//                throw new IllegalArgumentException("Checkpoint with id %d should've been a monitor checkpoint but was: %s".formatted(id, checkpoint.getClass()));
//            }
//        }

    @Override
    public void checkpointReached(int id) {
        Checkpoint checkpoint = checkpointRegister.checkpointById(id);
        Assert.assertNotNull("checkpoint not found: " + id, checkpoint);
        checkpoints.add(new RegularCheckpointReached(checkpoint, "", Thread.currentThread()));
    }

    @Override
    public void checkpointReached(int id, Object details) {
        Checkpoint checkpoint = checkpointRegister.checkpointById(id);
        Assert.assertNotNull("checkpoint not found: " + id, checkpoint);
        if (checkpoint instanceof MonitorCheckpoint monitorCheckpoint) {
            checkpoints.add(new MonitorCheckpointReached(monitorCheckpoint, details, Thread.currentThread()));
        } else {
            checkpoints.add(new RegularCheckpointReached(checkpoint, String.valueOf(details), Thread.currentThread()));
        }
    }

    @Override
    public void fieldAccessCheckpoint(int checkpointId, Object owner, Object value) {
        throw new RuntimeException("not yet implemented");
    }
}
