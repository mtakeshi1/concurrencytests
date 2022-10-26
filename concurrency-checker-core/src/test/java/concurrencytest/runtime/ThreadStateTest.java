package concurrencytest.runtime;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.MonitorCheckpointImpl;
import concurrencytest.runner.TestRuntimeState;
import concurrencytest.runtime.lock.BlockingResource;
import concurrencytest.runtime.lock.LockType;
import concurrencytest.runtime.thread.ThreadState;
import org.junit.Assert;
import org.junit.Test;

public class ThreadStateTest {

    @Test
    public void testMonitorReentrant() {
        MonitorCheckpointImpl mon = new MonitorCheckpointImpl(InjectionPoint.BEFORE, "", "", 1, "a", true);
        var ts = new ThreadState("actor").beforeMonitorAcquire(1, new Object(), mon);
        ThreadState locked = ts.monitorAcquired(1, "a", 1);
        TestRuntimeState state = new TestRuntimeState(locked);
        Assert.assertTrue(locked.canProceed(state));
        Assert.assertTrue(locked.ownedResources().contains(new BlockingResource(LockType.MONITOR, 1, Object.class, "", 1)));
        state = state.update(locked);
        Assert.assertTrue(locked.canProceed(state));
        Assert.assertTrue(locked.ownedResources().contains(new BlockingResource(LockType.MONITOR, 1, Object.class, "", 1)));
        ThreadState waiter = locked.beforeMonitorAcquire(1, new Object(), mon);
        Assert.assertFalse(waiter.blockedBy().isEmpty());
        ThreadState afterAcquired = waiter.monitorAcquired(1, "a", 1);
        Assert.assertTrue(afterAcquired.canProceed(state));
        state = state.update(afterAcquired);
        Assert.assertTrue(afterAcquired.canProceed(state));
    }

}