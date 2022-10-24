package concurrencytest.runtime.lock;

import concurrencytest.checkpoint.description.CheckpointDescription;
import concurrencytest.runtime.RuntimeState;
import concurrencytest.runtime.thread.ThreadState;
import org.junit.runner.notification.RunListener;

import java.util.Collection;

public interface BlockCause {

    int resourceId();

    BlockCauseType type();

    Collection<? extends ThreadState> blockedBy(RuntimeState state);

    default boolean isBlocked(ThreadState actor, RuntimeState state) {
        Collection<? extends ThreadState> ownedResources = blockedBy(state);
        return ownedResources.stream().anyMatch(s -> !s.actorName().equals(actor.actorName()));
    }

    default boolean isRunnable(ThreadState actor, RuntimeState state) {
        return !isBlocked(actor, state);
    }

    CheckpointDescription acquisitionPoint();

}
