package concurrencytest.runtime.thread;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.lock.*;
import concurrencytest.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

/**
 * Unmutable thread state snapshot
 * <p>
 * TODO add information about how many times it has waited. Not sure how we will reset that
 * <p>
 *    Actually, lets make ThreadState an interface, with impls:
 *    - RunnableThread
 *    - BlockedThread / LockedThread
 *    - Waiting
 *    - IO Maybe?
 * <p>
 * after another thread has been selected
 */
public record ThreadState(String actorName, int checkpoint, int loopCount,
                          List<BlockingResource> ownedResources, Optional<BlockCause> blockedBy, boolean finished) {

    public static final int MAX_ACTOR_NAME_LENGTH = 255;
    public static final int MAX_OWNED_RESOURCES = 16;

    public ThreadState(String actorName) {
        this(actorName, 0);
    }

    public ThreadState(String actorName, int checkpoint) {
        this(actorName, checkpoint, 0, Collections.emptyList(), Optional.empty(), false);
    }

    public ThreadState(String actorName, int checkpoint, int loopCount, List<BlockingResource> ownedResources, BlockCause blockedBy, boolean finished) {
        this(actorName, checkpoint, loopCount, ownedResources, Optional.ofNullable(blockedBy), finished);
    }

    public ThreadState {
        if (Objects.requireNonNull(actorName).length() > 255) {
            throw new IllegalArgumentException("name cannot have length > %d".formatted(MAX_ACTOR_NAME_LENGTH));
        }
        if (ownedResources.size() > MAX_OWNED_RESOURCES) {
            throw new IllegalArgumentException("At most %d resources can be held, but was: %d".formatted(MAX_OWNED_RESOURCES, ownedResources.size()));
        }
        Objects.requireNonNull(blockedBy, "blockedBy cannot be null. Use an Optional.empty() instead");
    }

    public boolean canProceed(RuntimeState state) {
//        if(state.checkpointRegister().isWaitOrAwait(checkpoint) && !state.allowSpuriousWakeup(this.actorName())) {
//              TODO we may need to add another state thing
//        }
        return this.blockedBy().map(cause -> cause.isRunnable(this, state)).orElse(true);
    }

    private void assertNotFinished() {
        if (finished()) {
            throw new IllegalStateException("tried operation on finished actor: %s".formatted(this.actorName));
        }
    }

    public ThreadState beforeMonitorAcquire(int monitorId, Object monitorOwner, CheckpointDescription description) {
        assertNotFinished();
        this.blockedBy().ifPresent(ignored -> {
            throw new IllegalStateException("actor %s is already waiting for %s but its requesting another monitor: %d".formatted(actorName, ignored, monitorId));
        });

        return new ThreadState(actorName, checkpoint, loopCount, ownedResources, new MonitorBlockCause(monitorId, monitorOwner, description), false);
    }

    public ThreadState monitorAcquired(int monitorId, String sourceCode, int lineNumber) {
        assertNotFinished();
        return this.blockedBy.filter(cause -> cause.type() == BlockCauseType.MONITOR && cause.resourceId() == monitorId)
                .map(c -> (MonitorBlockCause) c)
                .map(blockCause -> new ThreadState(actorName, checkpoint, loopCount, CollectionUtils.copyAndAdd(ownedResources, new BlockingResource(LockType.MONITOR, monitorId, blockCause.monitorOwner().getClass(), sourceCode, lineNumber)), Optional.empty(), false))
                .orElseThrow(() -> new IllegalStateException("actor %s was not waiting for monitor %d but tried to acquire (was waiting for %s)".formatted(actorName, monitorId, blockedBy())));
    }

    public ThreadState beforeLockAcquisition(int lockId, Lock lock, CheckpointDescription checkpointDescription) {
        assertNotFinished();
        this.blockedBy().ifPresent(ignored -> {
            throw new IllegalStateException("actor %s is already waiting for %s but its requesting another lock: %d".formatted(actorName, ignored, lockId));
        });
        return new ThreadState(actorName, checkpoint, loopCount, ownedResources, new LockBlockCause(lockId, lock, checkpointDescription), false);
    }

    public ThreadState lockTryAcquire(int lockId, boolean succeed, String sourceCode, int lineNumber) {
        assertNotFinished();
        return this.blockedBy.filter(cause -> cause.type() == BlockCauseType.LOCK && cause.resourceId() == lockId)
                .map(c -> (LockBlockCause) c)
                .map(blockCause ->
                        new ThreadState(actorName, checkpoint, loopCount, succeed ? CollectionUtils.copyAndAdd(ownedResources, new BlockingResource(LockType.LOCK, lockId, blockCause.lock().getClass(), sourceCode, lineNumber)) : ownedResources, Optional.empty(), false))
                .orElseThrow(() -> new IllegalStateException("actor %s was not waiting for monitor %d but tried to acquire (was waiting for %s)".formatted(actorName, lockId, blockedBy())));
    }

    public ThreadState lockReleased(int lockId) {
        assertNotFinished();
        List<BlockingResource> newResources = CollectionUtils.removeFirst(ownedResources, res -> res.lockType() == LockType.LOCK && res.resourceId() == lockId);
        if (newResources.size() == ownedResources.size()) {
            throw new IllegalStateException("actor %s tried to release a lock (%d) that it did not held (%s)".formatted(actorName, lockId, ownedResources));
        }
        return new ThreadState(actorName, checkpoint, loopCount, newResources, blockedBy, false);
    }

    public ThreadState monitorReleased(int monitorId) {
        assertNotFinished();
        List<BlockingResource> newResources = CollectionUtils.removeFirst(ownedResources, res -> res.lockType() == LockType.MONITOR && res.resourceId() == monitorId);
        if (newResources.size() == ownedResources.size()) {
            throw new IllegalStateException("actor %s tried to release a lock (%d) that it did not held (%s)".formatted(actorName, monitorId, ownedResources));
        }
        return new ThreadState(actorName, checkpoint, loopCount, newResources, blockedBy, false);
    }

    public ThreadState newCheckpointReached(CheckpointReached newCheckpoint, boolean actorFinishCheckpoint) {
        return new ThreadState(actorName, newCheckpoint.checkpointId(), newCheckpoint.checkpointId() == this.checkpoint() ? loopCount + 1 : 0, ownedResources, blockedBy, actorFinishCheckpoint);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ThreadState{");
        sb.append("'").append(actorName).append('\'');
        sb.append(", checkpoint=").append(checkpoint);
        if (loopCount != 0) {
            sb.append(", loopCount=").append(loopCount);
        }
        if (!ownedResources.isEmpty()) {
            sb.append(", ownedMonitors=").append(ownedResources);
        }
        blockedBy.ifPresent(cause -> sb.append(", blockingCause=").append(cause));
        if (finished) {
            sb.append(", FINISHED");
        }
        sb.append('}');
        return sb.toString();
    }
}
