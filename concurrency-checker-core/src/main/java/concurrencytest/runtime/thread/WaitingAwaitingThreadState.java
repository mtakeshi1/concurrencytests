package concurrencytest.runtime.thread;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.lock.BlockingResource;
import concurrencytest.runtime.lock.LockBlockCause;
import concurrencytest.runtime.lock.LockType;
import concurrencytest.runtime.lock.MonitorBlockCause;

import java.util.List;
import java.util.concurrent.locks.Lock;

public class WaitingAwaitingThreadState extends AbstractThreadState {

    private final CheckpointDescription waitCheckpoint;
    private final BlockingResource waitingCondition;

    public WaitingAwaitingThreadState(String actorName, int checkpoint, int loopCount, List<BlockingResource> ownedResources, CheckpointDescription acquisitionPoint, BlockingResource waitingCondition) {
        super(actorName, checkpoint, loopCount, ownedResources);
        this.waitCheckpoint = acquisitionPoint;
        this.waitingCondition = waitingCondition;
    }

    @Override
    public boolean canProceed(RuntimeState state) {
        return state.getWaitCount(this, waitCheckpoint, waitingCondition.resourceId(), waitingCondition.lockType()) == 0
                || state.isNotifySignalAvailable(waitingCondition.resourceId(), waitingCondition.lockType() == LockType.MONITOR);
    }

    @Override
    public ThreadState beforeMonitorAcquire(int monitorId, Object monitorOwner, CheckpointDescription description) {
        return new BeforeResourceAcquisitionThreadState(actorName(), checkpoint(), loopCount(), ownedResources(), new MonitorBlockCause(monitorId, monitorOwner, description));
    }

    @Override
    public ThreadState beforeLockAcquisition(int lockId, Lock lock, CheckpointDescription checkpointDescription) {
        return new BeforeResourceAcquisitionThreadState(actorName(), checkpoint(), loopCount(), ownedResources(), new LockBlockCause(lockId, lock, checkpointDescription));
    }

    @Override
    public ThreadState lockReleased(int lockId) {
        throw new IllegalStateException("Cannot release monitor while waiting on resource");
    }

    @Override
    public ThreadState monitorReleased(int monitorId) {
        throw new IllegalStateException("Cannot release monitor while waiting on resource");
    }

    @Override
    public ThreadState newCheckpointReached(CheckpointReached newCheckpoint) {
//        return new RunnableThreadState(actorName(), newCheckpoint.checkpointId(), newCheckpoint.checkpointId() == this.checkpoint() ? loopCount() + 1 : 0, ownedResources());
        return new WaitingAwaitingThreadState(
                actorName(), newCheckpoint.checkpointId(), newCheckpoint.checkpointId() == this.checkpoint() ? loopCount() + 1 : 0, ownedResources(), waitCheckpoint, waitingCondition
        );
    }

    @Override
    public boolean finished() {
        return false;
    }
}
