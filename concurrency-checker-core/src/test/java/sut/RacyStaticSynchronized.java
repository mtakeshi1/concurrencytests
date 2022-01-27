package sut;

import concurrencytest.annotations.Actor;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class RacyStaticSynchronized {

    @Before
    public void before() {
        StaticSynchronizedValueHolder.reset();
    }

    @Actor
    public void actor1() {
        StaticSynchronizedValueHolder.increment();
    }

    @Actor
    public void actor2() {
        StaticSynchronizedValueHolder.increment();
    }

    @After
    public void check() {
        Assert.assertEquals(2, StaticSynchronizedValueHolder.getValue());
    }


}
