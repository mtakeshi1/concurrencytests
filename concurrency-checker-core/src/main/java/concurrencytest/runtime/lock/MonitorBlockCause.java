package concurrencytest.runtime.lock;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;
import concurrencytest.util.CollectionUtils;

import java.util.Collection;

public record MonitorBlockCause(int resourceId, Object monitorOwner, CheckpointDescription acquisitionPoint) implements BlockCause {
    @Override
    public BlockCauseType type() {
        return BlockCauseType.MONITOR;
    }

    @Override
    public Collection<? extends ThreadState> ownedBy(RuntimeState state) {
        return CollectionUtils.nonNull(state.ownedResources().get(new BlockingResource(LockType.MONITOR, this.resourceId, monitorOwner.getClass(), acquisitionPoint.sourceFile(), acquisitionPoint.lineNumber())));
    }

    @Override
    public Class<?> resourceClass() {
        return monitorOwner.getClass();
    }
}
