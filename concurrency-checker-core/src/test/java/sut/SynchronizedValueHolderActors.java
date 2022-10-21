package sut;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class SynchronizedValueHolderActors {

    private SynchronizedValueHolder valueHolder;

    @Before
    public void setup() {
        valueHolder = new SynchronizedValueHolder();
    }

    @Actor
    public void actor1() {
        valueHolder.increment();
    }

    @Invariant
    public void valueBounds() {
        Assert.assertTrue(valueHolder.getValue() == 0 || valueHolder.getValue() == 1 || valueHolder.getValue() == 2);
    }

    @After
    public void after() {
        Assert.assertEquals(2, valueHolder.getValue());
    }

    @Actor
    public void actor2() {
        valueHolder.increment();
    }

}
