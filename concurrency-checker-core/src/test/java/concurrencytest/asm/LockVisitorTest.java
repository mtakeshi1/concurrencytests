package concurrencytest.asm;

import concurrencytest.asm.testClasses.LockTest;
import concurrencytest.reflection.ReflectionHelper;
import concurrencytest.runner.RecordingCheckpointRuntime;
import org.junit.Test;

import java.util.Objects;

public class LockVisitorTest extends BaseClassVisitorTest {


    @Test
    public void testTryLockWithTime() throws Exception {
        Class<?> injected = super.prepare(LockTest.class, (c, cv) -> new LockVisitor(cv, register, LockTest.class, ReflectionHelper.getInstance()));
        Runnable run = (Runnable) injected.getConstructor().newInstance();
        RecordingCheckpointRuntime managedRuntime = execute(run, Runnable::run);

    }



}
