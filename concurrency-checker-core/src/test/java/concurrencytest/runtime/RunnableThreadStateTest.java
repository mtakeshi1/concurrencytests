package concurrencytest.runtime;

import concurrencytest.annotations.InjectionPoint;
import concurrencytest.checkpoint.description.MonitorCheckpointDescriptionImpl;
import concurrencytest.runner.TestRuntimeState;
import concurrencytest.runtime.lock.BlockingResource;
import concurrencytest.runtime.lock.LockType;
import concurrencytest.runtime.thread.RunnableThreadState;
import concurrencytest.runtime.thread.ThreadState;
import org.junit.Assert;
import org.junit.Test;

import java.net.URL;
import java.util.Arrays;

public class RunnableThreadStateTest {

    @Test
    public void testMonitorReentrant() {
        MonitorCheckpointDescriptionImpl mon = new MonitorCheckpointDescriptionImpl(InjectionPoint.BEFORE, "", "", 1, true);
        var ts = new RunnableThreadState("actor").beforeMonitorAcquire(1, new Object(), mon);
        ThreadState locked = ts.monitorAcquired(1, "a", 1);
        TestRuntimeState state = new TestRuntimeState(locked);
        Assert.assertTrue(locked.canProceed(state));
        Assert.assertTrue(locked.ownedResources().contains(new BlockingResource(LockType.MONITOR, 1, Object.class, "", 1)));
        state = state.update(locked);
        Assert.assertTrue(locked.canProceed(state));
        Assert.assertTrue(locked.ownedResources().contains(new BlockingResource(LockType.MONITOR, 1, Object.class, "", 1)));
        RunnableThreadState waiter = (RunnableThreadState) locked.beforeMonitorAcquire(1, new Object(), mon);
        ThreadState afterAcquired = waiter.monitorAcquired(1, "a", 1);
        Assert.assertTrue(afterAcquired.canProceed(state));
        state = state.update(afterAcquired);
        Assert.assertTrue(afterAcquired.canProceed(state));
    }

    private static URL pathFor(Class<?> t) {
        return t.getResource("/" + t.getName().replace('.', '/') + ".class");
    }

    @Test
    public void testA() {
        System.out.println(pathFor(String.class));
        System.out.println(pathFor(RunnableThreadStateTest.class));
        System.out.println(pathFor(RunnableThreadState.class));

        Arrays.stream(System.getProperty("java.class.path").split(":")).forEach(System.out::println);

    }


}