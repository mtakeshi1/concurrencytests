package concurrencytest.runtime.lock;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;
import concurrencytest.util.CollectionUtils;

import java.util.Collection;
import java.util.concurrent.locks.Lock;

public record LockBlockCause(int resourceId, Lock lock, CheckpointDescription acquisitionPoint) implements BlockCause {
    @Override
    public BlockCauseType type() {
        return BlockCauseType.LOCK;
    }

    @Override
    public boolean isBlocked(ThreadState actor, RuntimeState state) {
        if(actor.ownedResources().contains(new BlockingResource(LockType.LOCK, resourceId, Lock.class, "", 1))) {
            return true;
        }
        if(lock.tryLock()) {
            lock.unlock();
            return true;
        }
        return false;
    }

    @Override
    public Collection<? extends ThreadState> blockedBy(RuntimeState state) {
        return CollectionUtils.nonNull(state.ownedResources().get(new BlockingResource(LockType.LOCK, this.resourceId, lock.getClass(), acquisitionPoint.sourceFile(), acquisitionPoint.lineNumber())));
    }

}
