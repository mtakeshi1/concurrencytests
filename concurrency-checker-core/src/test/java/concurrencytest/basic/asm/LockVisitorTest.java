package concurrencytest.basic.asm;

import concurrencytest.asm.LockCheckpointVisitor;
import concurrencytest.basic.asm.testClasses.LockTest;
import concurrencytest.basic.asm.testClasses.ReadWriteLockTest;
import concurrencytest.reflection.ReflectionHelper;
import concurrencytest.runner.RecordingCheckpointRuntime;
import org.junit.Assert;
import org.junit.Test;

public class LockVisitorTest extends BaseClassVisitorTest {


    @Test
    public void testTryLockWithTime() throws Exception {
        Class<?> injected = super.prepare(LockTest.class, (c, cv) -> new LockCheckpointVisitor(cv, register, LockTest.class, ReflectionHelper.getInstance()));
        Runnable run = (Runnable) injected.getConstructor().newInstance();
        RecordingCheckpointRuntime managedRuntime = execute(run, Runnable::run);
        var list = managedRuntime.getCheckpoints();
        Assert.assertFalse(list.isEmpty());
        System.out.println(list.size());
    }

    @Test
    public void testReadWriteLock() throws Exception {
        Class<?> injected = super.prepare(ReadWriteLockTest .class, (c, cv) -> new LockCheckpointVisitor(cv, register, LockTest.class, ReflectionHelper.getInstance()));
        Runnable run = (Runnable) injected.getConstructor().newInstance();
        RecordingCheckpointRuntime managedRuntime = execute(run, Runnable::run);
        var list = managedRuntime.getCheckpoints();
        Assert.assertFalse(list.isEmpty());
        System.out.println(list.size());
    }

}
