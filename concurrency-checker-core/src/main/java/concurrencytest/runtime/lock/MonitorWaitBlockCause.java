package concurrencytest.runtime.lock;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;

import java.util.Collection;
import java.util.Collections;

public record MonitorWaitBlockCause(int resourceId, CheckpointDescription acquisitionPoint) implements BlockCause {

    @Override
    public BlockCauseType type() {
        return BlockCauseType.MONITOR_WAIT;
    }

    @Override
    public boolean isBlocked(ThreadState actor, RuntimeState state) {
        return state.getWaitCount(actor, acquisitionPoint, resourceId, LockType.MONITOR) >= state.configuration().maxSpuriousWakeups()
                && !state.isNotifySignalAvailable(resourceId, type() == BlockCauseType.MONITOR_WAIT);
    }

    @Override
    public Collection<? extends ThreadState> blockedBy(RuntimeState state) {
        return Collections.emptyList();
    }

}
