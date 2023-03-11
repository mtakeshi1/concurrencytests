package concurrencytest.runtime.thread;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.checkpoint.instance.CheckpointReached;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.lock.BlockCause;
import concurrencytest.runtime.lock.BlockingResource;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;

public interface ThreadState {

    int MAX_ACTOR_NAME_LENGTH = 255;
    int MAX_OWNED_RESOURCES = 16;

    boolean canProceed(RuntimeState state);

    ThreadState beforeMonitorAcquire(int monitorId, Object monitorOwner, CheckpointDescription description);

    ThreadState beforeLockAcquisition(int lockId, Lock lock, CheckpointDescription checkpointDescription);

    default ThreadState monitorAcquired(int monitorId, String sourceCode, int lineNumber) {
        throw new IllegalStateException("actor %s was not waiting for monitor %d but tried to acquire it".formatted(actorName(), monitorId));
    }

    default ThreadState lockTryAcquire(int lockId, boolean succeed, String sourceCode, int lineNumber) {
        throw new IllegalStateException("actor %s was not waiting for Lock %d but tried to lock it".formatted(actorName(), lockId));
    }

    ThreadState lockReleased(int lockId);

    ThreadState monitorReleased(int monitorId);

    ThreadState newCheckpointReached(CheckpointReached newCheckpoint);

    default ThreadState actorFinished(CheckpointReached newCheckpoint) {
        return new FinishedThreadState(this.actorName(), newCheckpoint.checkpointId());
    }

    String actorName();

    int checkpoint();

    int loopCount();

    List<BlockingResource> ownedResources();

    boolean finished();

    default Optional<BlockCause> resourceDependency() {
        return Optional.empty();
    }

}
