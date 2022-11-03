package concurrencytest.asm;

import concurrencytest.asm.testClasses.LockTest;
import concurrencytest.reflection.ReflectionHelper;
import concurrencytest.runner.RecordingCheckpointRuntime;
import org.junit.Test;

public class LockVisitorTest extends BaseClassVisitorTest {


    @Test
    public void testTryLockWithTime() throws Exception {
        Class<?> injected = super.prepare(LockTest.class, (c, cv) -> new LockCheckpointVisitor(cv, register, LockTest.class, ReflectionHelper.getInstance()));
        Runnable run = (Runnable) injected.getConstructor().newInstance();
        RecordingCheckpointRuntime managedRuntime = execute(run, Runnable::run);

    }



}
