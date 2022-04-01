package agent.test;

import concurrencytest.ConcurrencyRunner;
import concurrencytest.annotations.Actor;
import concurrencytest.annotations.InstrumentationStrategy;
import concurrencytest.annotations.TestParameters;
import org.junit.After;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(ConcurrencyRunner.class)
@TestParameters(instrumentationStrategy = InstrumentationStrategy.ATTACH_AGENT)
public class TestAgent {

    private int value;

    @Actor
    public void actor1() {
        value += 2;
    }

    @Actor
    public void actor2() {
        value += 2;
    }

    @After
    public void after() {
        Assert.assertEquals(4, value);
    }

}
