package concurrencytest.v2.test;

import concurrencytest.basic.asm.testClasses.MonitorWait;
import org.junit.Ignore;
import org.junit.Test;
import sut.*;

/**
 * Basic sanity checks to see if the runner is not throwing errors on cases where it should run to completion.
 */
public class BasicPassingTests extends AbstractRunnerTests {

    @Test
    public void atomicCounter() {
        runToCompletion(AtomicIncrementTest.class);
    }

    @Test
    public void synchronizedCounter() {
        runToCompletion(SynchronizedMethodCounter.class);
    }

    @Test
    public void testStartNewThreads() {
        runToCompletion(NewThreadTest.class);
    }

    @Test
    public void synchronizedValueHolderMethod() {
        runToCompletion(SynchronizedValueHolderActors.class);
    }

    @Test
    public void synchronizedIndyWithMethodRef() {
        runToCompletion(RacyIndySynchronizedMethodRef.class);
    }

    @Test
    public void synchronizedBlocks() {
        runToCompletion(ExceptionalRacingCondition.class);
    }

    @Test
    public void lockUnlock() {
        runToCompletion(LockUnlock.class);
    }

    @Test
//    @Ignore("for now it's not working ")
    public void syncWaitNotify() {
        runToCompletion(MonitorWait.class);
    }


}
