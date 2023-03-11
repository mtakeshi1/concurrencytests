package concurrencytest.runtime.thread;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.lock.BlockCause;
import concurrencytest.runtime.lock.BlockCauseType;
import concurrencytest.runtime.lock.BlockingResource;
import concurrencytest.runtime.lock.LockType;
import concurrencytest.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public class BeforeResourceAcquisitionThreadState extends AbstractThreadState {
    private final BlockCause resource;

    public BeforeResourceAcquisitionThreadState(String actorName, int checkpoint, int loopCount, List<BlockingResource> ownedResources, BlockCause resource) {
        super(actorName, checkpoint, loopCount, ownedResources);
        this.resource = Objects.requireNonNull(resource, "resource cannot be null");
    }

    @Override
    public boolean canProceed(RuntimeState state) {
        return resource.isRunnable(this, state);
    }

    @Override
    public ThreadState beforeMonitorAcquire(int monitorId, Object monitorOwner, CheckpointDescription description) {
        throw new IllegalStateException("actor %s was already waiting for %s but tried to acquire monitor %d".formatted(actorName(), resource, monitorId));
    }

    @Override
    public ThreadState beforeLockAcquisition(int lockId, Lock lock, CheckpointDescription checkpointDescription) {
        throw new IllegalStateException("actor %s was already waiting for %s but tried to lock Lock %d".formatted(actorName(), resource, lockId));
    }

    @Override
    public ThreadState monitorAcquired(int monitorId, String sourceCode, int lineNumber) {
        if (resource.matches(BlockCauseType.MONITOR, monitorId)) {
            var res = new BlockingResource(LockType.MONITOR, monitorId, resource.resourceClass(), sourceCode, lineNumber);
            return new RunnableThreadState(actorName(), checkpoint(), loopCount(), CollectionUtils.copyAndAdd(ownedResources(), res));
        } else {
            throw new IllegalStateException("actor %s was waiting for %s but tried acquired monitor %d".formatted(actorName(), resource, monitorId));
        }
    }

    @Override
    public ThreadState lockTryAcquire(int lockId, boolean succeed, String sourceCode, int lineNumber) {
        if (resource.matches(BlockCauseType.LOCK, lockId)) {
            var res = new BlockingResource(LockType.LOCK, lockId, resource.resourceClass(), sourceCode, lineNumber);
            return new RunnableThreadState(actorName(), checkpoint(), loopCount(), CollectionUtils.copyAndAdd(ownedResources(), res));
        } else {
            throw new IllegalStateException("actor %s was waiting for %s but tried acquired lock %d".formatted(actorName(), resource, lockId));
        }
    }

    @Override
    public ThreadState lockReleased(int lockId) {
        throw new IllegalStateException("actor %s was waiting for %s but tried to unlock %d".formatted(actorName(), resource, lockId));
    }

    @Override
    public ThreadState monitorReleased(int monitorId) {
        throw new IllegalStateException("actor %s was waiting for %s but tried to release monitor %d".formatted(actorName(), resource, monitorId));
    }

    @Override
    public ThreadState newCheckpointReached(CheckpointReached newCheckpoint) {
//        throw new IllegalStateException("actor %s tried to move to another checkpoint (%d) before acquiring %s".formatted(actorName(), newCheckpoint.checkpointId(), resource));
        return new BeforeResourceAcquisitionThreadState(actorName(), newCheckpoint.checkpointId(), loopCount(), ownedResources(), resource);
    }

    @Override
    public boolean finished() {
        return false;
    }

    @Override
    public Optional<BlockCause> resourceDependency() {
        return Optional.of(resource);
    }
}
