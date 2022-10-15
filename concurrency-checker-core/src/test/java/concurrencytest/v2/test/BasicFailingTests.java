package concurrencytest.v2.test;

import org.junit.Test;

public class BasicFailingTests extends AbstractRunnerTests {

    @Test
    public void sharedCounterExampleShouldFail() {
        runExpectError(SimpleSharedCounter.class, e -> e instanceof AssertionError);
    }



}
