package concurrencytest.v2.test;

import concurrencytest.runner.DeadlockFoundException;
import concurrencytest.runner.InitialPathBlockedException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import sut.RacyActorsGetters;
import sut.SynchronizedValueHolder;
import sut.TryLockUnlock;

import java.util.List;

public class DeadlockCheck extends AbstractRunnerTests {


    @Test
    public void testEnsureDeadLock() {
        runExpectError(DeadlockTest.class, List.of("actor1", "actor1", "actor2"), e -> e instanceof DeadlockFoundException);
    }

    @Test
    @Ignore("this test shouldn't deadlock. It should throw AssertionError")
    public void testEnsureDeadLockSynchronizedMethods() {
        runExpectError(RacyActorsGetters.class, List.of("actor1", "actor1", "actor1", "actor1", "actor1", "actor2", "actor2", "actor2"), e -> e instanceof DeadlockFoundException, RacyActorsGetters.class, SynchronizedValueHolder.class);
    }

    @Test
    @Ignore("initial path blocked exception is no longer being thrown due to reworking of the scheduler")
    public void testInitialPathBlocked() {
        runExpectError(DeadlockTest.class, List.of("actor1", "actor1", "actor1", "actor1", "actor2", "actor2"), e -> e instanceof InitialPathBlockedException);
    }

    @Test
    public void testShouldFindFailingCase() {
        runExpectError(TryLockUnlock.class, List.of("actors_0", "actors_0", "actors_1"), e -> e instanceof AssertionError);
    }


}
