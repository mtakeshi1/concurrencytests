package concurrencytest.runtime.thread;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.lock.BlockingResource;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.Lock;

public class FinishedThreadState extends AbstractThreadState {

    public FinishedThreadState(String actorName, int checkpoint, int loopCount, List<BlockingResource> ownedResources) {
        super(actorName, checkpoint, loopCount, ownedResources);
        if (!ownedResources.isEmpty()) {
            throw new IllegalStateException("finished thread cannot be owning resources");
        }
    }

    public FinishedThreadState(String actorName, int checkpoint, int loopCount) {
        this(actorName, checkpoint, loopCount, Collections.emptyList());
    }

    public FinishedThreadState(String actorName, int checkpoint) {
        this(actorName, checkpoint, 0);
    }

    public FinishedThreadState(String actorName) {
        this(actorName, 0);

    }

    @Override
    public boolean canProceed(RuntimeState state) {
        return false;
    }

    @Override
    public ThreadState beforeMonitorAcquire(int monitorId, Object monitorOwner, CheckpointDescription description) {
        throw new IllegalStateException("thread finished");
    }

    @Override
    public ThreadState monitorAcquired(int monitorId, String sourceCode, int lineNumber) {
        throw new IllegalStateException("thread finished");
    }

    @Override
    public ThreadState beforeLockAcquisition(int lockId, Lock lock, CheckpointDescription checkpointDescription) {
        throw new IllegalStateException("thread finished");
    }

    @Override
    public ThreadState lockTryAcquire(int lockId, boolean succeed, String sourceCode, int lineNumber) {
        throw new IllegalStateException("thread finished");
    }

    @Override
    public ThreadState lockReleased(int lockId) {
        throw new IllegalStateException("thread finished");
    }

    @Override
    public ThreadState monitorReleased(int monitorId) {
        throw new IllegalStateException("thread finished");
    }

    @Override
    public ThreadState newCheckpointReached(CheckpointReached newCheckpoint) {
        throw new IllegalStateException("thread finished");
    }

    @Override
    public boolean finished() {
        return true;
    }
}
