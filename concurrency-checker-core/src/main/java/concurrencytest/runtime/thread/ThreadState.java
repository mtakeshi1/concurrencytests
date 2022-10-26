package concurrencytest.runtime.thread;

import concurrencytest.checkpoint.description.LockAcquireCheckpointDescription;
import concurrencytest.checkpoint.description.MonitorCheckpointDescription;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.checkpoint.CheckpointReached;
import concurrencytest.runtime.lock.*;
import concurrencytest.runtime.lock.BlockCauseType;
import concurrencytest.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.locks.Lock;

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

    public Collection<ThreadState> dependencies(RuntimeState state) {
//        var monitor = waitingForMonitor.map(LockMonitorAcquisition::lockOrMonitorId).filter(monId -> !state.ownedMonitors().containsKey(monId)).map(mon -> state.ownedMonitors().get(mon)).stream();
//        var locks = waitingForLock.map(LockMonitorAcquisition::lockOrMonitorId).filter(monId -> !state.lockedLocks().containsKey(monId)).map(mon -> state.lockedLocks().get(mon)).stream();
//        var conditionalWait = waitingForThread.map(actor -> state.actorNamesToThreadStates().get(actor)).stream();
//        return Stream.concat(monitor, Stream.concat(locks, conditionalWait)).toList();
        return Collections.emptyList(); //TODO
    }

    public boolean canProceed(RuntimeState state) {
        return this.blockedBy().map(cause -> cause.isRunnable(this, state)).orElse(true);
    }

    private void assertNotFinished() {
        if (finished()) {
            throw new IllegalStateException("tried operation on finished actor: %s".formatted(this.actorName));
        }
    }

    public ThreadState beforeMonitorAcquire(int monitorId, Object monitorOwner, MonitorCheckpointDescription description) {
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

    public ThreadState beforeLockAcquisition(int lockId, Lock lock, LockAcquireCheckpointDescription checkpointDescription) {
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
        assertNotFinished();
        List<BlockingResource> newResources = CollectionUtils.removeFirst(ownedResources, res -> res.lockType() == LockType.MONITOR && res.resourceId() == monitorId);
        if (newResources.size() == ownedResources.size()) {
            throw new IllegalStateException("actor %s tried to release a lock (%d) that it did not held (%s)".formatted(actorName, monitorId, ownedResources));
        }
        return new ThreadState(actorName, checkpoint, loopCount, newResources, blockedBy, false);
    }

    public ThreadState newCheckpointReached(CheckpointReached newCheckpoint, boolean lastCheckpoint) {
        return new ThreadState(actorName, newCheckpoint.checkpointId(), newCheckpoint.checkpointId() == this.checkpoint() ? loopCount + 1 : 0, ownedResources, blockedBy, lastCheckpoint);
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
