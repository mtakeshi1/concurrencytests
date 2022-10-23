package concurrencytest.runtime.lock;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;

import java.util.Collection;

public interface BlockCause {

    int resourceId();

    BlockCauseType type();

    Collection<? extends ThreadState> blockedBy(RuntimeState state);

    default boolean isBlocked(RuntimeState state) {
        return !blockedBy(state).isEmpty();
    }

    CheckpointDescription acquisitionPoint();

}
