package concurrencytest.v2.test;

import concurrencytest.runner.DeadlockFoundException;
import org.junit.Test;
import sut.*;
import sut.RacyActorsFieldAccess.ValueHolder;
import sut.mock.Session;
import sut.mock.SessionManager;
import sut.mock.SessionState;

public class BasicFailingTests extends AbstractRunnerTests {

    @Test
    public void sharedCounterExampleShouldFail() {
        runExpectError(SimpleSharedCounter.class, e -> e instanceof AssertionError);
    }

    @Test
    public void deadLockTest() {
        runExpectError(DeadlockTest.class, e -> e instanceof DeadlockFoundException);
    }

    @Test
    public void nestedSyncBlocks() {
        runExpectError(NestedSyncBlocks.class, e -> e instanceof DeadlockFoundException, SessionManager.class, Session.class, SessionState.class);
    }

    @Test
    public void innerClassField() {
        runExpectError(RacyActorsFieldAccess.class, e -> e instanceof AssertionError, ValueHolder.class);
    }

    @Test
    public void racyGetters() {
        runExpectError(RacyActorsGetters.class, e -> e instanceof AssertionError, SynchronizedValueHolder.class);
    }

    @Test
    public void testLambda() {
        runExpectError(RacyIndyLambda.class, e -> e instanceof AssertionError);
    }

    @Test
    public void tryLockUnlock() {
        runExpectError(TryLockUnlock.class, e -> e instanceof AssertionError);
    }
}
