package sut;

import concurrencytest.annotations.Actor;
import concurrencytest.annotations.Invariant;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;

public class RacyActorsFieldAccess {

    public static class ValueHolder {
        public int value;
    }

    private ValueHolder shared;

    @Before
    public void before() {
        shared = new ValueHolder();
    }

    @Invariant
    public void sharedIsNeverNull() {
        Assert.assertNotNull(shared);
    }

    @Invariant
    public void sharedIsNeverNegative() {
        Assert.assertTrue(shared.value >= 0);
    }

    @Actor
    public void actor1() {
        shared.value++;
    }

    @Actor
    public void actor2() {
        shared.value++;
    }

    @After
    public void check() {
        Assert.assertEquals(2, shared.value);
    }

}
