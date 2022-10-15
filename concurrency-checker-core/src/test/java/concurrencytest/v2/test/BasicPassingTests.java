package concurrencytest.v2.test;

import org.junit.Test;

/**
 * Basic sanity checks to see if the runner is not throwing errors on cases where it should run to completion.
 *
 */
public class BasicPassingTests extends AbstractRunnerTests {

    @Test
    public void synchronizedCounter() {
        runToCompletion(SynchronizedMethodCounter.class);
    }

}
