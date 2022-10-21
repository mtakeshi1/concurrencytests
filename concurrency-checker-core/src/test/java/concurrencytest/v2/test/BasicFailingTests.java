package concurrencytest.v2.test;

import concurrencytest.runner.DeadlockFoundException;
import org.junit.Assert;
import org.junit.Test;
import sut.NestedSyncBlocks;
import sut.RacyActorsFieldAccess;
import sut.RacyActorsGetters;
import sut.RacyIndyLambda;

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
        runExpectError(NestedSyncBlocks.class, e -> e instanceof DeadlockFoundException);
    }

    @Test
    public void innerClassField() {
        runExpectError(RacyActorsFieldAccess.class, e -> e instanceof AssertionError);
    }

    @Test
    public void racyGetters() {
        runExpectError(RacyActorsGetters.class, e -> e instanceof AssertionError);
    }

    @Test
    public void testLambda() {
        runExpectError(RacyIndyLambda.class, e -> e instanceof AssertionError);
    }

}
