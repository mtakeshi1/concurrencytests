package concurrencytest.runtime;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.LockAcquireCheckpointDescription;
import concurrencytest.runner.TestRuntimeState;
import concurrencytest.runtime.lock.BlockingResource;
import concurrencytest.runtime.lock.LockBlockCause;
import concurrencytest.runtime.lock.LockType;
import concurrencytest.runtime.thread.RunnableThreadState;
import concurrencytest.runtime.tree.ActorInformation;
import concurrencytest.runtime.tree.offheap.ByteBufferBackedTreeNode;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class MonitorLockTest {

    @Test
    public void tryAcquireShouldBeUnlocked() {
        LockBlockCause cause = new LockBlockCause(1, new ReentrantLock(), new LockAcquireCheckpointDescription(InjectionPoint.BEFORE, "", "", -1, true, false));
        var ts = new RunnableThreadState("a", 1, 0, List.of());
        var ts2 = new RunnableThreadState("b", 1, 0, List.of(new BlockingResource(LockType.LOCK, 1, ReentrantLock.class, "", -1)));
        TestRuntimeState state = new TestRuntimeState(ts, ts2);
        Assert.assertFalse(cause.isBlocked(ts, state));
        Assert.assertTrue(cause.isRunnable(ts, state));
        Collection<ActorInformation> actorInformations = ByteBufferBackedTreeNode.toActorInformation(state, List.of(ts, ts2));
        Assert.assertTrue(actorInformations.size() > 0);
        Assert.assertTrue(actorInformations.stream().noneMatch(ActorInformation::isBlocked));
    }


}
