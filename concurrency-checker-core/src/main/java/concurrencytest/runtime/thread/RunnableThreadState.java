package concurrencytest.runtime.thread;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.lock.BlockingResource;
import concurrencytest.runtime.lock.LockBlockCause;
import concurrencytest.runtime.lock.LockType;
import concurrencytest.runtime.lock.MonitorBlockCause;
import concurrencytest.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;

/**
 * Unmutable thread state snapshot
 * <p>
 * TODO add information about how many times it has waited. Not sure how we will reset that
 * <p>
 *    Actually, lets make RunnableThreadState an interface, with impls:
 *    - RunnableThread
 *    - BlockedThread / LockedThread
 *    - Waiting
 *    - IO Maybe?
 * <p>
 * after another thread has been selected
 */
public record RunnableThreadState(String actorName, int checkpoint, int loopCount, List<BlockingResource> ownedResources) implements ThreadState {


    public RunnableThreadState(String actorName) {
        this(actorName, 0);
    }

    public RunnableThreadState(String actorName, int checkpoint) {
        this(actorName, checkpoint, 0);
    }

    public RunnableThreadState(String actorName, int checkpoint, int loopCount) {
        this(actorName, checkpoint, loopCount, Collections.emptyList());
    }

    public RunnableThreadState {
        if (Objects.requireNonNull(actorName).length() > 255) {
            throw new IllegalArgumentException("name cannot have length > %d".formatted(MAX_ACTOR_NAME_LENGTH));
        }
        if (ownedResources.size() > MAX_OWNED_RESOURCES) {
            throw new IllegalArgumentException("At most %d resources can be held, but was: %d".formatted(MAX_OWNED_RESOURCES, ownedResources.size()));
        }
    }

    @Override
    public boolean canProceed(RuntimeState state) {
        return true;
    }

    private void assertNotFinished() {
        if (finished()) {
            throw new IllegalStateException("tried operation on finished actor: %s".formatted(this.actorName));
        }
    }

    @Override
    public ThreadState beforeLockAcquisition(int lockId, Lock lock, CheckpointDescription checkpointDescription) {
        return new BeforeResourceAcquisitionThreadState(actorName(), checkpoint(), loopCount(), ownedResources(), new LockBlockCause(lockId, lock, checkpointDescription));
    }

    @Override
    public ThreadState beforeMonitorAcquire(int monitorId, Object monitorOwner, CheckpointDescription description) {
        return new BeforeResourceAcquisitionThreadState(actorName(), checkpoint(), loopCount(), ownedResources(), new MonitorBlockCause(monitorId, monitorOwner, description));
    }

    @Override
    public ThreadState lockReleased(int lockId) {
        assertNotFinished();
        List<BlockingResource> newResources = CollectionUtils.removeFirst(ownedResources, res -> res.lockType() == LockType.LOCK && res.resourceId() == lockId);
        if (newResources.size() == ownedResources.size()) {
            throw new IllegalStateException("actor %s tried to release a lock (%d) that it did not held (%s)".formatted(actorName, lockId, ownedResources));
        }
        return new RunnableThreadState(actorName, checkpoint, loopCount, newResources);
    }

    @Override
    public ThreadState monitorReleased(int monitorId) {
        assertNotFinished();
        List<BlockingResource> newResources = CollectionUtils.removeFirst(ownedResources, res -> res.lockType() == LockType.MONITOR && res.resourceId() == monitorId);
        if (newResources.size() == ownedResources.size()) {
            throw new IllegalStateException("actor %s tried to release a lock (%d) that it did not held (%s)".formatted(actorName, monitorId, ownedResources));
        }
        return new RunnableThreadState(actorName, checkpoint, loopCount, newResources);
    }

    @Override
    public ThreadState newCheckpointReached(CheckpointReached newCheckpoint) {
        return new RunnableThreadState(actorName(), newCheckpoint.checkpointId(), newCheckpoint.checkpointId() == this.checkpoint() ? loopCount() + 1 : 0, ownedResources());
    }

    @Override
    public boolean finished() {
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RunnableThreadState{");
        sb.append("'").append(actorName).append('\'');
        sb.append(", checkpoint=").append(checkpoint);
        if (loopCount != 0) {
            sb.append(", loopCount=").append(loopCount);
        }
        if (!ownedResources.isEmpty()) {
            sb.append(", ownedMonitors=").append(ownedResources);
        }
//        blockedBy.ifPresent(cause -> sb.append(", blockingCause=").append(cause));
        sb.append('}');
        return sb.toString();
    }
}
