package concurrencytest.v2.test;

import concurrencytest.exception.DeadlockFoundException;
import org.junit.Test;

public class BasicFailingTests extends AbstractRunnerTests {

    @Test
    public void sharedCounterExampleShouldFail() {
        runExpectError(SimpleSharedCounter.class, e -> e instanceof AssertionError);
    }

    @Test
    public void deadLockTest() {
        runExpectError(DeadlockTest.class, e -> e instanceof DeadlockFoundException);
    }

}
